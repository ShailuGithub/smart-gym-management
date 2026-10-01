-- Smart Gym Management - SQL Server schema
-- Run this in SQL Server Management Studio (or sqlcmd) connected to your SQL Server instance as sa.

IF DB_ID('SmartGymDB') IS NULL
BEGIN
    CREATE DATABASE SmartGymDB;
END
GO

USE SmartGymDB;
GO

IF OBJECT_ID('dbo.api_tokens', 'U') IS NOT NULL DROP TABLE dbo.api_tokens;
IF OBJECT_ID('dbo.reminders', 'U') IS NOT NULL DROP TABLE dbo.reminders;
IF OBJECT_ID('dbo.payments', 'U') IS NOT NULL DROP TABLE dbo.payments;
IF OBJECT_ID('dbo.member_subscriptions', 'U') IS NOT NULL DROP TABLE dbo.member_subscriptions;
IF OBJECT_ID('dbo.members', 'U') IS NOT NULL DROP TABLE dbo.members;
IF OBJECT_ID('dbo.membership_plans', 'U') IS NOT NULL DROP TABLE dbo.membership_plans;
IF OBJECT_ID('dbo.audit_log', 'U') IS NOT NULL DROP TABLE dbo.audit_log;
IF OBJECT_ID('dbo.password_reset_tokens', 'U') IS NOT NULL DROP TABLE dbo.password_reset_tokens;
IF OBJECT_ID('dbo.app_settings', 'U') IS NOT NULL DROP TABLE dbo.app_settings;
IF OBJECT_ID('dbo.users', 'U') IS NOT NULL DROP TABLE dbo.users;
IF OBJECT_ID('dbo.gym_branches', 'U') IS NOT NULL DROP TABLE dbo.gym_branches;
GO

-- One row per gym branch/location. An ADMIN user (branch_id NULL on their own row) can see all
-- of these; a MANAGER/STAFF user is tied to exactly one via users.branch_id.
CREATE TABLE gym_branches (
    branch_id    INT IDENTITY(1,1) PRIMARY KEY,
    name         VARCHAR(150) NOT NULL,
    address      VARCHAR(300),
    city         VARCHAR(100),
    state        VARCHAR(100),
    phone        VARCHAR(20),
    email        VARCHAR(100),
    status       VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE
    created_at   DATETIME NOT NULL DEFAULT GETDATE()
);
GO

CREATE TABLE users (
    user_id                INT IDENTITY(1,1) PRIMARY KEY,
    branch_id              INT NULL FOREIGN KEY REFERENCES gym_branches(branch_id), -- NULL = ADMIN (sees every branch); set = MANAGER/STAFF scoped to that branch
    username               VARCHAR(50)  NOT NULL UNIQUE,
    password               VARCHAR(100) NOT NULL,
    full_name              VARCHAR(100),
    email                  VARCHAR(100),
    role                   VARCHAR(20)  NOT NULL DEFAULT 'STAFF', -- ADMIN, MANAGER, STAFF
    can_create             BIT NOT NULL DEFAULT 1, -- only meaningful for MANAGER/STAFF; ADMIN always has full rights
    can_edit               BIT NOT NULL DEFAULT 1,
    can_delete             BIT NOT NULL DEFAULT 0,
    failed_login_attempts  INT NOT NULL DEFAULT 0,
    locked_until           DATETIME NULL,
    created_at             DATETIME NOT NULL DEFAULT GETDATE()
);
GO

CREATE TABLE password_reset_tokens (
    token_id     INT IDENTITY(1,1) PRIMARY KEY,
    user_id      INT NOT NULL FOREIGN KEY REFERENCES users(user_id),
    token        VARCHAR(100) NOT NULL UNIQUE,
    expires_at   DATETIME NOT NULL,
    used         BIT NOT NULL DEFAULT 0,
    created_at   DATETIME NOT NULL DEFAULT GETDATE()
);
GO

-- Opaque bearer tokens issued to the Flutter app on login (no session cookies on mobile).
CREATE TABLE api_tokens (
    token_id     INT IDENTITY(1,1) PRIMARY KEY,
    user_id      INT NOT NULL FOREIGN KEY REFERENCES users(user_id),
    token        VARCHAR(100) NOT NULL UNIQUE,
    expires_at   DATETIME NOT NULL,
    created_at   DATETIME NOT NULL DEFAULT GETDATE()
);
GO

CREATE TABLE audit_log (
    log_id        INT IDENTITY(1,1) PRIMARY KEY,
    branch_id     INT NULL,
    user_id       INT NULL,
    username      VARCHAR(50),
    action        VARCHAR(20) NOT NULL, -- CREATE, UPDATE, DELETE, LOGIN, etc.
    entity_type   VARCHAR(30) NOT NULL, -- BRANCH, MEMBER, PLAN, SUBSCRIPTION, PAYMENT, REMINDER, USER, SETTINGS
    entity_id     INT NULL,
    description   VARCHAR(500),
    created_at    DATETIME NOT NULL DEFAULT GETDATE()
);
GO

-- Business-wide membership plans (not per-branch) - e.g. "1 Month", "3 Months", "1 Year".
CREATE TABLE membership_plans (
    plan_id       INT IDENTITY(1,1) PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    duration_days INT NOT NULL,
    price         DECIMAL(12,2) NOT NULL DEFAULT 0,
    description   VARCHAR(300),
    status        VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE
    created_at    DATETIME NOT NULL DEFAULT GETDATE()
);
GO

CREATE TABLE members (
    member_id     INT IDENTITY(1,1) PRIMARY KEY,
    branch_id     INT NOT NULL FOREIGN KEY REFERENCES gym_branches(branch_id),
    full_name     VARCHAR(100) NOT NULL,
    phone         VARCHAR(20),
    email         VARCHAR(100),
    gender        VARCHAR(10),
    dob           DATE NULL,
    address       VARCHAR(300),
    photo_path    VARCHAR(300), -- generated filename under upload.dir, not user-supplied
    join_date     DATE NOT NULL DEFAULT CAST(GETDATE() AS DATE),
    status        VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE
    created_at    DATETIME NOT NULL DEFAULT GETDATE()
);
GO

-- One row per plan purchase/renewal. The member's CURRENT plan is whichever row has the latest
-- end_date with status='ACTIVE' - there is no separate "current plan" column to keep in sync.
CREATE TABLE member_subscriptions (
    subscription_id  INT IDENTITY(1,1) PRIMARY KEY,
    member_id        INT NOT NULL FOREIGN KEY REFERENCES members(member_id),
    branch_id        INT NOT NULL FOREIGN KEY REFERENCES gym_branches(branch_id),
    plan_id          INT NOT NULL FOREIGN KEY REFERENCES membership_plans(plan_id),
    start_date       DATE NOT NULL,
    end_date         DATE NOT NULL,
    amount_charged   DECIMAL(12,2) NOT NULL DEFAULT 0,
    status           VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, EXPIRED, CANCELLED
    created_at       DATETIME NOT NULL DEFAULT GETDATE()
);
GO

CREATE TABLE payments (
    payment_id       INT IDENTITY(1,1) PRIMARY KEY,
    branch_id        INT NOT NULL FOREIGN KEY REFERENCES gym_branches(branch_id),
    member_id        INT NOT NULL FOREIGN KEY REFERENCES members(member_id),
    subscription_id  INT NULL FOREIGN KEY REFERENCES member_subscriptions(subscription_id),
    payment_date     DATE NOT NULL DEFAULT CAST(GETDATE() AS DATE),
    amount           DECIMAL(12,2) NOT NULL,
    mode             VARCHAR(30), -- CASH, UPI, BANK_TRANSFER, CARD, CHEQUE
    reference_no     VARCHAR(50),
    remarks          VARCHAR(300),
    created_at       DATETIME NOT NULL DEFAULT GETDATE()
);
GO

-- Log of dues/expiry reminders sent (or attempted) to a member. Actual delivery goes through
-- the pluggable SmsService (see src/sms) - the default implementation only logs, matching the
-- dependency-free "dev mode" approach used for Forgot Password.
CREATE TABLE reminders (
    reminder_id   INT IDENTITY(1,1) PRIMARY KEY,
    member_id     INT NOT NULL FOREIGN KEY REFERENCES members(member_id),
    branch_id     INT NOT NULL FOREIGN KEY REFERENCES gym_branches(branch_id),
    type          VARCHAR(20) NOT NULL, -- EXPIRY, DUE
    channel       VARCHAR(20) NOT NULL DEFAULT 'SMS', -- SMS, PUSH
    message       VARCHAR(500),
    status        VARCHAR(20) NOT NULL DEFAULT 'SENT', -- SENT, FAILED
    sent_at       DATETIME NOT NULL DEFAULT GETDATE(),
    created_at    DATETIME NOT NULL DEFAULT GETDATE()
);
GO

-- Singleton row (id = 1) holding business-wide info shown on the Settings screen.
CREATE TABLE app_settings (
    id                          INT PRIMARY KEY DEFAULT 1,
    business_name               VARCHAR(150) NOT NULL DEFAULT 'My Gym Business',
    address                     VARCHAR(300),
    phone                       VARCHAR(20),
    email                       VARCHAR(100),
    currency_symbol             VARCHAR(5) NOT NULL DEFAULT 'Rs.',
    reminder_days_before_expiry INT NOT NULL DEFAULT 3, -- flag a member as "expiring soon" this many days before their subscription ends
    sms_enabled                 BIT NOT NULL DEFAULT 0, -- 0 = reminders only get logged, never actually sent
    updated_at                  DATETIME NOT NULL DEFAULT GETDATE(),
    CONSTRAINT chk_app_settings_singleton CHECK (id = 1)
);
GO

INSERT INTO app_settings (id, business_name) VALUES (1, 'My Gym Business');
GO

-- Seed the admin account (branch_id NULL - an ADMIN isn't tied to any one branch, they see all
-- of them). Default password is 'admin123' (bcrypt-hashed below) - log in and change it immediately.
INSERT INTO users (branch_id, username, password, full_name, role, can_create, can_edit, can_delete)
VALUES (NULL, 'admin', '$2a$10$dHcmEuPfqSwPYs.ZnxDM0.gDofgAQ37bNMD4L5QIytPLoCJfqhA.u', 'System Administrator', 'ADMIN', 1, 1, 1);
GO

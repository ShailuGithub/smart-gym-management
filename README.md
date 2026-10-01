# Smart Gym Management

A multi-branch gym management system: a Java web application (servlets/JSP on SQL Server) with a REST API, and a Flutter mobile companion app.

> Portfolio showcase. Database credentials, server addresses and local config have been removed.
> Copy `src/db.properties.example` to `src/db.properties` and set your own values.

## Features
- Member management, membership plans and subscriptions, with active / expiring / expired status
- Payments and payment history, due-member tracking and reminders (pluggable SMS service)
- Multi-branch support: staff are scoped to a branch, admins see all branches
- Role and permission model (admin, staff, create/edit/delete rights), password change and reset, login rate limiting
- Reports with CSV export, an audit log of changes and an app-settings screen
- REST API with token authentication, used by the mobile app
- Flutter app: dashboard, members, plans, payments, reminders, reports and settings

## Tech stack
| Layer | Technology |
|---|---|
| Web | Java, Servlets, JSP, Filters (GlassFish) |
| Data | SQL Server (`mssql-jdbc`), DAO pattern, `jBCrypt` password hashing, Gson |
| Mobile | Flutter / Dart (`http`, `shared_preferences`) |

## Structure
```
src/        servlets, REST API servlets, DAOs, models, services, filters
web/        JSP views and assets
sql/        schema.sql and migrations
mobile/     Flutter app
```

## Running it
1. Create a SQL Server database and run `sql/schema.sql`, then the files in `sql/migrations` in order.
2. Create `src/db.properties` from the example.
3. Add the libraries `gson`, `jbcrypt` and `mssql-jdbc` to `lib/` and build with NetBeans/Ant. Deploy the WAR to GlassFish.
4. The seeded admin account is created by `schema.sql`. **Change its password immediately.**
5. Mobile: `cd mobile && flutter pub get`, set the server address in `lib/services/api_client.dart`, then `flutter run`.

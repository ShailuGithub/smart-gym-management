import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../models/payment.dart';
import '../services/api_client.dart';

/// Branch-scoped payments ledger (GET /api/payments?branchId=), newest first per
/// api.PaymentApiServlet/dao.PaymentDAO.findByBranch.
class PaymentsScreen extends StatefulWidget {
  final ApiClient apiClient;
  final int branchId;

  const PaymentsScreen({super.key, required this.apiClient, required this.branchId});

  @override
  State<PaymentsScreen> createState() => _PaymentsScreenState();
}

class _PaymentsScreenState extends State<PaymentsScreen> {
  List<Payment> _payments = [];
  bool _loading = true;
  String? _error;
  final _dateFormat = DateFormat('dd MMM yyyy');

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final raw = await widget.apiClient
          .get('/payments', query: {'branchId': widget.branchId.toString()}) as List<dynamic>;
      setState(() => _payments = raw.map((e) => Payment.fromJson(e as Map<String, dynamic>)).toList());
    } catch (e) {
      setState(() => _error = 'Failed to load payments: $e');
    } finally {
      setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) return const Center(child: CircularProgressIndicator());
    if (_error != null) return Center(child: Text(_error!));
    if (_payments.isEmpty) {
      return RefreshIndicator(
        onRefresh: _load,
        child: ListView(children: const [
          Padding(padding: EdgeInsets.all(32), child: Center(child: Text('No payments recorded yet.')))
        ]),
      );
    }
    return RefreshIndicator(
      onRefresh: _load,
      child: ListView.separated(
        itemCount: _payments.length,
        separatorBuilder: (_, _) => const Divider(height: 1),
        itemBuilder: (context, i) {
          final p = _payments[i];
          return ListTile(
            title: Text(p.memberName ?? 'Member #${p.memberId}'),
            subtitle: Text('${_dateFormat.format(p.paymentDate)} - ${p.mode ?? 'N/A'}'
                '${p.referenceNo != null && p.referenceNo!.isNotEmpty ? ' (${p.referenceNo})' : ''}'),
            trailing: Text(
              'Rs. ${p.amount.toStringAsFixed(0)}',
              style: const TextStyle(fontWeight: FontWeight.bold),
            ),
          );
        },
      ),
    );
  }
}

import 'package:flutter/material.dart';
import 'package:bfsi_app/core/theme/app_colors.dart';
import 'package:bfsi_app/core/theme/app_text_styles.dart';
import 'package:bfsi_app/features/dashboard/domain/entities/account_entity.dart';

class AccountCard extends StatelessWidget {
  final AccountEntity account;

  const AccountCard({super.key, required this.account});

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                _AccountTypeChip(type: account.type),
                Icon(_accountIcon(account.type),
                    color: AppColors.primary, size: 28),
              ],
            ),
            const SizedBox(height: 16),
            Text(
              '${account.currency} ${_formatBalance(account.balance)}',
              style: AppTextStyles.amount,
            ),
            const SizedBox(height: 4),
            Text('Available Balance', style: AppTextStyles.caption),
            const Divider(height: 24),
            Text(
              _maskAccountNumber(account.accountNumber),
              style: AppTextStyles.bodyMedium.copyWith(
                letterSpacing: 2,
                fontFamily: 'monospace',
              ),
            ),
            const SizedBox(height: 4),
            Text(account.holderName, style: AppTextStyles.labelLarge),
          ],
        ),
      ),
    );
  }

  String _formatBalance(double balance) {
    if (balance >= 1e7) return '${(balance / 1e7).toStringAsFixed(2)} Cr';
    if (balance >= 1e5) return '${(balance / 1e5).toStringAsFixed(2)} L';
    return balance.toStringAsFixed(2);
  }

  String _maskAccountNumber(String num) {
    if (num.length <= 4) return num;
    return '${'•' * (num.length - 4)} ${num.substring(num.length - 4)}';
  }

  IconData _accountIcon(AccountType type) => switch (type) {
        AccountType.savings => Icons.savings_outlined,
        AccountType.current => Icons.account_balance_outlined,
        AccountType.fixed => Icons.lock_clock_outlined,
        AccountType.loan => Icons.credit_score_outlined,
      };
}

class _AccountTypeChip extends StatelessWidget {
  final AccountType type;
  const _AccountTypeChip({required this.type});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: AppColors.primary.withOpacity(0.1),
        borderRadius: BorderRadius.circular(20),
      ),
      child: Text(
        type.name.toUpperCase(),
        style: AppTextStyles.caption.copyWith(
          color: AppColors.primary,
          fontWeight: FontWeight.w600,
        ),
      ),
    );
  }
}

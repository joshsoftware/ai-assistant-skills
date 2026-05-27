import 'package:bfsi_app/features/dashboard/domain/entities/account_entity.dart';

class AccountModel extends AccountEntity {
  const AccountModel({
    required super.id,
    required super.accountNumber,
    required super.holderName,
    required super.type,
    required super.balance,
    required super.currency,
  });

  factory AccountModel.fromJson(Map<String, dynamic> json) => AccountModel(
        id: json['id'] as String,
        accountNumber: json['account_number'] as String,
        holderName: json['holder_name'] as String,
        type: _parseType(json['type'] as String),
        balance: (json['balance'] as num).toDouble(),
        currency: json['currency'] as String? ?? 'INR',
      );

  static AccountType _parseType(String raw) => switch (raw.toLowerCase()) {
        'savings' => AccountType.savings,
        'current' => AccountType.current,
        'fixed' => AccountType.fixed,
        'loan' => AccountType.loan,
        _ => AccountType.savings,
      };

  Map<String, dynamic> toJson() => {
        'id': id,
        'account_number': accountNumber,
        'holder_name': holderName,
        'type': type.name,
        'balance': balance,
        'currency': currency,
      };
}

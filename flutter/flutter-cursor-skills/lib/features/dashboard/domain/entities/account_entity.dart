import 'package:equatable/equatable.dart';

enum AccountType { savings, current, fixed, loan }

class AccountEntity extends Equatable {
  final String id;
  final String accountNumber;
  final String holderName;
  final AccountType type;
  final double balance;
  final String currency;

  const AccountEntity({
    required this.id,
    required this.accountNumber,
    required this.holderName,
    required this.type,
    required this.balance,
    required this.currency,
  });

  @override
  List<Object?> get props => [id, accountNumber, type, balance];
}

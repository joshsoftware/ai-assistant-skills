import 'package:dartz/dartz.dart';
import 'package:bfsi_app/core/error/failures.dart';
import 'package:bfsi_app/features/dashboard/domain/entities/account_entity.dart';
import 'package:bfsi_app/features/dashboard/domain/repositories/dashboard_repository.dart';

class GetAccountsUsecase {
  final DashboardRepository repository;
  const GetAccountsUsecase(this.repository);

  Future<Either<Failure, List<AccountEntity>>> call() => repository.getAccounts();
}

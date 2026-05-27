import 'package:dartz/dartz.dart';
import 'package:bfsi_app/core/error/failures.dart';
import 'package:bfsi_app/features/dashboard/domain/entities/account_entity.dart';

abstract class DashboardRepository {
  Future<Either<Failure, List<AccountEntity>>> getAccounts();
}

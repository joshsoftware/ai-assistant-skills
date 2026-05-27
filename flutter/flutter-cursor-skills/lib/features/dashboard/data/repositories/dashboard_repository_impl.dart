import 'package:dartz/dartz.dart';
import 'package:bfsi_app/core/error/exceptions.dart';
import 'package:bfsi_app/core/error/failures.dart';
import 'package:bfsi_app/features/dashboard/data/datasources/dashboard_remote_datasource.dart';
import 'package:bfsi_app/features/dashboard/domain/entities/account_entity.dart';
import 'package:bfsi_app/features/dashboard/domain/repositories/dashboard_repository.dart';

class DashboardRepositoryImpl implements DashboardRepository {
  final DashboardRemoteDataSource _remoteDataSource;
  const DashboardRepositoryImpl(this._remoteDataSource);

  @override
  Future<Either<Failure, List<AccountEntity>>> getAccounts() async {
    try {
      final accounts = await _remoteDataSource.getAccounts();
      return Right(accounts);
    } on UnauthorizedException {
      return const Left(UnauthorizedFailure());
    } on ServerException catch (e) {
      return Left(ServerFailure(message: e.message, statusCode: e.statusCode));
    } on NetworkException catch (e) {
      return Left(NetworkFailure(message: e.message));
    }
  }
}

import 'package:dartz/dartz.dart';
import 'package:bfsi_app/core/error/failures.dart';
import 'package:bfsi_app/features/auth/domain/entities/user_entity.dart';

abstract class AuthRepository {
  Future<Either<Failure, UserEntity>> login({
    required String email,
    required String password,
  });

  Future<Either<Failure, void>> logout();

  Future<Either<Failure, void>> forgotPassword({required String email});
}

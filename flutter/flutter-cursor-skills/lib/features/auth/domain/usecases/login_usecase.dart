import 'package:dartz/dartz.dart';
import 'package:bfsi_app/core/error/failures.dart';
import 'package:bfsi_app/features/auth/domain/entities/user_entity.dart';
import 'package:bfsi_app/features/auth/domain/repositories/auth_repository.dart';

class LoginParams {
  final String email;
  final String password;
  const LoginParams({required this.email, required this.password});
}

class LoginUsecase {
  final AuthRepository repository;
  const LoginUsecase(this.repository);

  Future<Either<Failure, UserEntity>> call(LoginParams params) =>
      repository.login(email: params.email, password: params.password);
}

import 'package:dartz/dartz.dart';
import 'package:bfsi_app/core/error/failures.dart';
import 'package:bfsi_app/features/auth/domain/repositories/auth_repository.dart';

class ForgotPasswordUsecase {
  final AuthRepository repository;
  const ForgotPasswordUsecase(this.repository);

  Future<Either<Failure, void>> call(String email) =>
      repository.forgotPassword(email: email);
}

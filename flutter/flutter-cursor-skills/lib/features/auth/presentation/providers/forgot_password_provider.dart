import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:bfsi_app/features/auth/domain/usecases/forgot_password_usecase.dart';
import 'package:bfsi_app/features/auth/presentation/providers/auth_provider.dart';

final forgotPasswordUsecaseProvider = Provider<ForgotPasswordUsecase>((ref) {
  return ForgotPasswordUsecase(ref.watch(authRepositoryProvider));
});

enum ForgotPasswordStatus { idle, loading, success, failure }

class ForgotPasswordState {
  final ForgotPasswordStatus status;
  final String? error;

  const ForgotPasswordState({
    this.status = ForgotPasswordStatus.idle,
    this.error,
  });

  ForgotPasswordState copyWith({ForgotPasswordStatus? status, String? error}) =>
      ForgotPasswordState(
        status: status ?? this.status,
        error: error,
      );
}

class ForgotPasswordNotifier extends StateNotifier<ForgotPasswordState> {
  final ForgotPasswordUsecase _usecase;

  ForgotPasswordNotifier(this._usecase) : super(const ForgotPasswordState());

  Future<void> sendResetLink(String email) async {
    state = state.copyWith(status: ForgotPasswordStatus.loading, error: null);
    final result = await _usecase(email);
    result.fold(
      (failure) => state = state.copyWith(
        status: ForgotPasswordStatus.failure,
        error: failure.message,
      ),
      (_) => state = state.copyWith(status: ForgotPasswordStatus.success),
    );
  }

  void reset() => state = const ForgotPasswordState();
}

final forgotPasswordProvider =
    StateNotifierProvider.autoDispose<ForgotPasswordNotifier, ForgotPasswordState>(
  (ref) => ForgotPasswordNotifier(ref.watch(forgotPasswordUsecaseProvider)),
);

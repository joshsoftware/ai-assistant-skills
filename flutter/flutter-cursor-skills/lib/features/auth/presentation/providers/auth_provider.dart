import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:bfsi_app/core/network/api_client.dart';
import 'package:bfsi_app/core/storage/secure_storage_service.dart';
import 'package:bfsi_app/features/auth/data/datasources/auth_remote_datasource.dart';
import 'package:bfsi_app/features/auth/data/repositories/auth_repository_impl.dart';
import 'package:bfsi_app/features/auth/domain/entities/user_entity.dart';
import 'package:bfsi_app/features/auth/domain/repositories/auth_repository.dart';
import 'package:bfsi_app/features/auth/domain/usecases/login_usecase.dart';

final authRepositoryProvider = Provider<AuthRepository>((ref) {
  final client = ref.watch(apiClientProvider);
  final storage = ref.watch(secureStorageServiceProvider);
  return AuthRepositoryImpl(AuthRemoteDataSourceImpl(client, storage));
});

final loginUsecaseProvider = Provider<LoginUsecase>((ref) {
  return LoginUsecase(ref.watch(authRepositoryProvider));
});

class AuthState {
  final UserEntity? user;
  final bool isLoading;
  final String? error;

  const AuthState({this.user, this.isLoading = false, this.error});

  AuthState copyWith({UserEntity? user, bool? isLoading, String? error}) =>
      AuthState(
        user: user ?? this.user,
        isLoading: isLoading ?? this.isLoading,
        error: error,
      );
}

class AuthNotifier extends StateNotifier<AuthState> {
  final LoginUsecase _loginUsecase;
  final AuthRepository _repository;

  AuthNotifier(this._loginUsecase, this._repository) : super(const AuthState());

  Future<bool> login({required String email, required String password}) async {
    state = state.copyWith(isLoading: true, error: null);
    final result = await _loginUsecase(
      LoginParams(email: email, password: password),
    );
    return result.fold(
      (failure) {
        state = state.copyWith(isLoading: false, error: failure.message);
        return false;
      },
      (user) {
        state = state.copyWith(isLoading: false, user: user);
        return true;
      },
    );
  }

  Future<void> logout() async {
    await _repository.logout();
    state = const AuthState();
  }
}

final authProvider = StateNotifierProvider<AuthNotifier, AuthState>((ref) {
  return AuthNotifier(
    ref.watch(loginUsecaseProvider),
    ref.watch(authRepositoryProvider),
  );
});

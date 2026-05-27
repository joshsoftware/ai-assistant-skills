import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:bfsi_app/core/network/api_client.dart';
import 'package:bfsi_app/features/dashboard/data/datasources/dashboard_remote_datasource.dart';
import 'package:bfsi_app/features/dashboard/data/repositories/dashboard_repository_impl.dart';
import 'package:bfsi_app/features/dashboard/domain/entities/account_entity.dart';
import 'package:bfsi_app/features/dashboard/domain/repositories/dashboard_repository.dart';
import 'package:bfsi_app/features/dashboard/domain/usecases/get_accounts_usecase.dart';

final dashboardRepositoryProvider = Provider<DashboardRepository>((ref) {
  final client = ref.watch(apiClientProvider);
  return DashboardRepositoryImpl(DashboardRemoteDataSourceImpl(client));
});

final getAccountsUsecaseProvider = Provider<GetAccountsUsecase>((ref) {
  return GetAccountsUsecase(ref.watch(dashboardRepositoryProvider));
});

class DashboardState {
  final List<AccountEntity> accounts;
  final bool isLoading;
  final String? error;

  const DashboardState({
    this.accounts = const [],
    this.isLoading = false,
    this.error,
  });

  DashboardState copyWith({
    List<AccountEntity>? accounts,
    bool? isLoading,
    String? error,
  }) =>
      DashboardState(
        accounts: accounts ?? this.accounts,
        isLoading: isLoading ?? this.isLoading,
        error: error,
      );

  double get totalBalance =>
      accounts.fold(0, (sum, a) => sum + a.balance);
}

class DashboardNotifier extends StateNotifier<DashboardState> {
  final GetAccountsUsecase _getAccounts;

  DashboardNotifier(this._getAccounts) : super(const DashboardState()) {
    loadAccounts();
  }

  Future<void> loadAccounts() async {
    state = state.copyWith(isLoading: true, error: null);
    final result = await _getAccounts();
    result.fold(
      (failure) => state = state.copyWith(isLoading: false, error: failure.message),
      (accounts) => state = state.copyWith(isLoading: false, accounts: accounts),
    );
  }
}

final dashboardProvider =
    StateNotifierProvider<DashboardNotifier, DashboardState>((ref) {
  return DashboardNotifier(ref.watch(getAccountsUsecaseProvider));
});

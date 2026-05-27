import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:bfsi_app/core/storage/secure_storage_service.dart';
import 'package:bfsi_app/features/auth/presentation/screens/forgot_password_screen.dart';
import 'package:bfsi_app/features/auth/presentation/screens/login_screen.dart';
import 'package:bfsi_app/features/dashboard/presentation/screens/dashboard_screen.dart';

final appRouterProvider = Provider<GoRouter>((ref) {
  final secureStorage = ref.watch(secureStorageServiceProvider);
  return GoRouter(
    initialLocation: AppRoutes.login,
    redirect: (context, state) async {
      final token = await secureStorage.getToken();
      final isLoggedIn = token != null && token.isNotEmpty;
      final isOnLogin = state.matchedLocation == AppRoutes.login;

      if (!isLoggedIn && !isOnLogin) return AppRoutes.login;
      if (isLoggedIn && isOnLogin) return AppRoutes.dashboard;
      return null;
    },
    routes: [
      GoRoute(
        path: AppRoutes.login,
        name: 'login',
        builder: (context, state) => const LoginScreen(),
      ),
      GoRoute(
        path: AppRoutes.forgotPassword,
        name: 'forgot-password',
        builder: (context, state) => const ForgotPasswordScreen(),
      ),
      GoRoute(
        path: AppRoutes.dashboard,
        name: 'dashboard',
        builder: (context, state) => const DashboardScreen(),
      ),
    ],
    errorBuilder: (context, state) => Scaffold(
      body: Center(child: Text('Route not found: ${state.error}')),
    ),
  );
});

class AppRoutes {
  AppRoutes._();
  static const login = '/login';
  static const forgotPassword = '/forgot-password';
  static const dashboard = '/dashboard';
}

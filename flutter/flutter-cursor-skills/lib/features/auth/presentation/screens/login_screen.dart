import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:bfsi_app/app/router/app_router.dart';
import 'package:bfsi_app/core/theme/app_colors.dart';
import 'package:bfsi_app/core/theme/app_text_styles.dart';
import 'package:bfsi_app/features/auth/presentation/widgets/login_form.dart';

class LoginScreen extends StatelessWidget {
  const LoginScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(horizontal: 24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const SizedBox(height: 60),
              _BrandHeader(),
              const SizedBox(height: 48),
              Text('Welcome Back', style: AppTextStyles.displayLarge),
              const SizedBox(height: 8),
              Text(
                'Sign in to your account to continue',
                style: AppTextStyles.bodyMedium,
              ),
              const SizedBox(height: 36),
              const LoginForm(),
              const SizedBox(height: 32),
              Center(
                child: TextButton(
                  onPressed: () => context.push(AppRoutes.forgotPassword),
                  child: const Text('Forgot Password?'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _BrandHeader extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Container(
          height: 48,
          width: 48,
          decoration: BoxDecoration(
            color: AppColors.primary,
            borderRadius: BorderRadius.circular(12),
          ),
          child: const Icon(Icons.account_balance, color: Colors.white, size: 28),
        ),
        const SizedBox(width: 12),
        Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'BFSI App',
              style: AppTextStyles.titleLarge.copyWith(color: AppColors.primary),
            ),
            Text('Secure Banking', style: AppTextStyles.caption),
          ],
        ),
      ],
    );
  }
}

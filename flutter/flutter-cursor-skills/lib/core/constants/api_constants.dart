class ApiConstants {
  ApiConstants._();

  static const String baseUrl = 'https://api.bfsi-app.com/v1';
  static const int connectTimeout = 30000;
  static const int receiveTimeout = 30000;

  static const String login = '/auth/login';
  static const String logout = '/auth/logout';
  static const String refreshToken = '/auth/refresh';
  static const String forgotPassword = '/auth/forgot-password';
  static const String accounts = '/accounts';
  static const String transactions = '/transactions';
  static const String profile = '/profile';
}

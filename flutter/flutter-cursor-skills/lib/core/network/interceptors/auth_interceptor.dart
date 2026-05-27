import 'package:dio/dio.dart';
import 'package:bfsi_app/core/constants/app_constants.dart';
import 'package:bfsi_app/core/storage/secure_storage_service.dart';

class AuthInterceptor extends Interceptor {
  final SecureStorageService _storage;

  AuthInterceptor(this._storage);

  @override
  Future<void> onRequest(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) async {
    final token = await _storage.getToken();
    if (token != null && token.isNotEmpty) {
      options.headers['Authorization'] = 'Bearer $token';
    }
    handler.next(options);
  }

  @override
  Future<void> onError(
    DioException err,
    ErrorInterceptorHandler handler,
  ) async {
    if (err.response?.statusCode == 401) {
      final refreshed = await _tryRefreshToken(err.requestOptions);
      if (refreshed != null) {
        handler.resolve(refreshed);
        return;
      }
      await _storage.clearAll();
    }
    handler.next(err);
  }

  Future<Response?> _tryRefreshToken(RequestOptions original) async {
    try {
      final refreshToken = await _storage.read(AppConstants.refreshTokenKey);
      if (refreshToken == null) return null;

      final dio = Dio();
      final response = await dio.post(
        '${original.baseUrl}/auth/refresh',
        data: {'refresh_token': refreshToken},
      );

      final newToken = response.data['access_token'] as String?;
      if (newToken == null) return null;

      await _storage.saveToken(newToken);
      original.headers['Authorization'] = 'Bearer $newToken';

      return await Dio().fetch(original);
    } catch (_) {
      return null;
    }
  }
}

import 'package:dio/dio.dart';
import 'package:bfsi_app/core/error/exceptions.dart';

class ErrorInterceptor extends Interceptor {
  @override
  void onError(DioException err, ErrorInterceptorHandler handler) {
    switch (err.type) {
      case DioExceptionType.connectionTimeout:
      case DioExceptionType.receiveTimeout:
      case DioExceptionType.sendTimeout:
        throw NetworkException(message: 'Connection timed out. Please try again.');
      case DioExceptionType.connectionError:
        throw NetworkException(message: 'No internet connection.');
      case DioExceptionType.badResponse:
        final statusCode = err.response?.statusCode;
        final message = _extractErrorMessage(err.response);
        if (statusCode == 401) throw const UnauthorizedException();
        throw ServerException(message: message, statusCode: statusCode);
      default:
        throw ServerException(message: err.message ?? 'Unexpected error occurred.');
    }
  }

  String _extractErrorMessage(Response? response) {
    try {
      final data = response?.data;
      if (data is Map) return data['message']?.toString() ?? 'Server error';
      return 'Server error';
    } catch (_) {
      return 'Server error';
    }
  }
}

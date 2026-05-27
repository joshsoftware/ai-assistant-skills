import 'package:bfsi_app/core/constants/api_constants.dart';
import 'package:bfsi_app/core/network/api_client.dart';
import 'package:bfsi_app/core/storage/secure_storage_service.dart';
import 'package:bfsi_app/features/auth/data/models/user_model.dart';

abstract class AuthRemoteDataSource {
  Future<UserModel> login({required String email, required String password});
  Future<void> logout();
  Future<void> forgotPassword({required String email});
}

class AuthRemoteDataSourceImpl implements AuthRemoteDataSource {
  final ApiClient _client;
  final SecureStorageService _storage;

  const AuthRemoteDataSourceImpl(this._client, this._storage);

  @override
  Future<UserModel> login({
    required String email,
    required String password,
  }) async {
    final response = await _client.post(
      ApiConstants.login,
      data: {'email': email, 'password': password},
    );
    final data = response.data as Map<String, dynamic>;
    await _storage.saveToken(data['access_token'] as String);
    await _storage.saveRefreshToken(data['refresh_token'] as String);
    return UserModel.fromJson(data['user'] as Map<String, dynamic>);
  }

  @override
  Future<void> logout() async {
    await _client.post(ApiConstants.logout);
    await _storage.clearAll();
  }

  @override
  Future<void> forgotPassword({required String email}) async {
    await _client.post(
      ApiConstants.forgotPassword,
      data: {'email': email},
    );
  }
}

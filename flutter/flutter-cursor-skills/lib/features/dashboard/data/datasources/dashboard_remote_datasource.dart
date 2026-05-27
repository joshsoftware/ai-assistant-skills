import 'package:bfsi_app/core/constants/api_constants.dart';
import 'package:bfsi_app/core/network/api_client.dart';
import 'package:bfsi_app/features/dashboard/data/models/account_model.dart';

abstract class DashboardRemoteDataSource {
  Future<List<AccountModel>> getAccounts();
}

class DashboardRemoteDataSourceImpl implements DashboardRemoteDataSource {
  final ApiClient _client;
  const DashboardRemoteDataSourceImpl(this._client);

  @override
  Future<List<AccountModel>> getAccounts() async {
    final response = await _client.get(ApiConstants.accounts);
    final list = response.data as List<dynamic>;
    return list
        .map((e) => AccountModel.fromJson(e as Map<String, dynamic>))
        .toList();
  }
}

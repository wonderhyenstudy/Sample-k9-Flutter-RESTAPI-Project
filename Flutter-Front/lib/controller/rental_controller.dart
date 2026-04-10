import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';
import '../model/rental_model.dart';
import '../const/api_constants.dart';
import '../my_app.dart';

/// 회원의 도서 대여/반납 상태를 추적 및 관리하는 컨트롤러
/// API: GET /api/rental?memberId={}&page=0&size=20
class RentalController extends ChangeNotifier {
  final FlutterSecureStorage _secureStorage = const FlutterSecureStorage();

  List<RentalModel> _rentalList = [];
  List<RentalModel> get rentalList => _rentalList;

  bool _isLoading = false;
  bool get isLoading => _isLoading;

  String? _errorMessage;
  String? get errorMessage => _errorMessage;

  Future<String?> _getAccessToken() async =>
      await _secureStorage.read(key: "accessToken");

  Future<String?> _getMemberId() async =>
      await _secureStorage.read(key: "memberId");

  /// 401 Unauthorized → 저장된 토큰 삭제 후 로그인 화면으로 이동
  Future<void> _handle401() async {
    await _secureStorage.deleteAll();
    MyApp.navigatorKey.currentState
        ?.pushNamedAndRemoveUntil('/login', (_) => false);
  }

  /// 내 전체 대여 목록 조회
  /// [memberId] 파라미터는 하위 호환용 — 실제로는 SecureStorage에서 읽음
  Future<void> fetchMemberRentals([int? legacyMemberId]) async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final String? accessToken = await _getAccessToken();
      final String? memberIdStr = await _getMemberId();

      if (accessToken == null || memberIdStr == null) {
        _errorMessage = '로그인이 필요합니다.';
        _rentalList = [];
        return;
      }

      final url = Uri.parse(
          '${ApiConstants.springBaseUrl}/rental?memberId=$memberIdStr&page=0&size=20');
      final response = await http.get(url, headers: {
        'Authorization': 'Bearer $accessToken',
        'Content-Type': 'application/json',
      });

      if (response.statusCode == 200) {
        final Map<String, dynamic> pageData =
            jsonDecode(utf8.decode(response.bodyBytes));
        final List<dynamic> content = pageData['content'] ?? [];
        _rentalList =
            content.map((json) => RentalModel.fromJson(json)).toList();
      } else if (response.statusCode == 401) {
        await _handle401();
      } else {
        _errorMessage = '대여 목록 조회 실패 (${response.statusCode})';
        _rentalList = [];
      }
    } catch (e) {
      _errorMessage = '네트워크 오류: $e';
      print('대여 목록 불러오기 에러: $e');
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  /// 도서 대여 신청
  /// API: POST /api/rental  Body: { "memberId": Long, "bookId": Long }
  Future<bool> rentBook(int bookId) async {
    try {
      final String? accessToken = await _getAccessToken();
      final String? memberIdStr = await _getMemberId();
      if (accessToken == null || memberIdStr == null) return false;

      final url = Uri.parse('${ApiConstants.springBaseUrl}/rental');
      final response = await http.post(
        url,
        headers: {
          'Authorization': 'Bearer $accessToken',
          'Content-Type': 'application/json',
        },
        body: jsonEncode({
          'memberId': int.parse(memberIdStr),
          'bookId': bookId,
        }),
      );

      if (response.statusCode == 200 || response.statusCode == 201) {
        await fetchMemberRentals();
        return true;
      }
      return false;
    } catch (e) {
      print('대여 신청 에러: $e');
      return false;
    }
  }
}

import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';
import '../model/apply_model.dart';
import '../const/api_constants.dart';

/// 도서관 시설(스터디룸 등)의 예약 신청 데이터를 관리하는 컨트롤러
/// API:
///   GET  /api/apply/my?memberId={}&page=0&size=20  - 내 예약 목록
///   POST /api/apply?memberId={}                    - 시설 예약 신청
class ReserveController extends ChangeNotifier {
  final FlutterSecureStorage _secureStorage = const FlutterSecureStorage();

  List<ApplyModel> _reservationList = [];
  List<ApplyModel> get reservationList => _reservationList;

  bool _isLoading = false;
  bool get isLoading => _isLoading;

  String? _errorMessage;
  String? get errorMessage => _errorMessage;

  Future<String?> _getAccessToken() async =>
      await _secureStorage.read(key: "accessToken");

  Future<String?> _getMemberId() async =>
      await _secureStorage.read(key: "memberId");

  /// 내 시설 예약 목록 조회
  Future<void> fetchReservations({int page = 0, int size = 20}) async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final String? accessToken = await _getAccessToken();
      final String? memberIdStr = await _getMemberId();

      if (accessToken == null || memberIdStr == null) {
        _errorMessage = '로그인이 필요합니다.';
        _reservationList = [];
        return;
      }

      final url = Uri.parse(
          '${ApiConstants.springBaseUrl}/apply/my?memberId=$memberIdStr&page=$page&size=$size');
      final response = await http.get(url, headers: {
        'Authorization': 'Bearer $accessToken',
        'Content-Type': 'application/json',
      });

      if (response.statusCode == 200) {
        final Map<String, dynamic> pageData =
            jsonDecode(utf8.decode(response.bodyBytes));
        final List<dynamic> content = pageData['content'] ?? [];
        _reservationList =
            content.map((json) => ApplyModel.fromJson(json)).toList();
      } else {
        _errorMessage = '예약 목록 조회 실패 (${response.statusCode})';
        _reservationList = [];
      }
    } catch (e) {
      _errorMessage = '네트워크 오류: $e';
      print('예약 목록 불러오기 에러: $e');
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  /// 시설 예약 신청
  /// API: POST /api/apply?memberId={} + body: {facilityType, reserveDate, applicantName, phone, participants}
  Future<bool> createReservation({
    required String facilityType,
    required String reserveDate,
    required String applicantName,
    String phone = '',
    int participants = 1,
  }) async {
    try {
      final String? accessToken = await _getAccessToken();
      final String? memberIdStr = await _getMemberId();
      if (accessToken == null || memberIdStr == null) return false;

      final url = Uri.parse(
          '${ApiConstants.springBaseUrl}/apply?memberId=$memberIdStr');
      final response = await http.post(
        url,
        headers: {
          'Authorization': 'Bearer $accessToken',
          'Content-Type': 'application/json',
        },
        body: jsonEncode({
          'facilityType': facilityType,
          'reserveDate': reserveDate,
          'applicantName': applicantName,
          'phone': phone,
          'participants': participants,
        }),
      );

      if (response.statusCode == 201) {
        await fetchReservations(); // 목록 갱신
        return true;
      }
      return false;
    } catch (e) {
      print('예약 신청 에러: $e');
      return false;
    }
  }
}

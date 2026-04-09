import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';
import '../model/event_model.dart';
import '../const/api_constants.dart';

/// 도서관 행사 목록 및 참가 신청을 관리하는 컨트롤러
/// API: GET /api/event?page=0&size=20
class EventController extends ChangeNotifier {
  final FlutterSecureStorage _secureStorage = const FlutterSecureStorage();

  List<LibraryEventModel> _eventList = [];
  List<LibraryEventModel> get eventList => _eventList;

  bool _isLoading = false;
  bool get isLoading => _isLoading;

  String? _errorMessage;
  String? get errorMessage => _errorMessage;

  Future<String?> _getAccessToken() async =>
      await _secureStorage.read(key: "accessToken");

  Future<String?> _getMemberId() async =>
      await _secureStorage.read(key: "memberId");

  /// 행사 목록 조회 (행사 시작일 기준 내림차순)
  Future<void> fetchEvents({int page = 0, int size = 20}) async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final String? accessToken = await _getAccessToken();
      final url = Uri.parse(
          '${ApiConstants.springBaseUrl}/event?page=$page&size=$size');

      final response = await http.get(url, headers: {
        if (accessToken != null) 'Authorization': 'Bearer $accessToken',
        'Content-Type': 'application/json',
      });

      if (response.statusCode == 200) {
        final Map<String, dynamic> pageData =
            jsonDecode(utf8.decode(response.bodyBytes));
        final List<dynamic> content = pageData['content'] ?? [];
        _eventList =
            content.map((json) => LibraryEventModel.fromJson(json)).toList();
      } else {
        _errorMessage = '행사 목록 조회 실패 (${response.statusCode})';
        _eventList = [];
      }
    } catch (e) {
      _errorMessage = '네트워크 오류: $e';
      print('행사 리스트 불러오기 에러: $e');
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  /// 행사 참가 신청
  /// API: POST /api/event/{eventId}/apply?memberId={memberId}
  Future<bool> applyEvent(int eventId) async {
    try {
      final String? accessToken = await _getAccessToken();
      final String? memberIdStr = await _getMemberId();
      if (accessToken == null || memberIdStr == null) return false;

      final url = Uri.parse(
          '${ApiConstants.springBaseUrl}/event/$eventId/apply?memberId=$memberIdStr');
      final response = await http.post(url, headers: {
        'Authorization': 'Bearer $accessToken',
      });

      return response.statusCode == 201;
    } catch (e) {
      print('행사 신청 에러: $e');
      return false;
    }
  }
}

import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';
import '../model/inquiry_model.dart';
import '../const/api_constants.dart';

/// 사용자 1:1 문의사항 데이터와 전송 기능을 관리하는 컨트롤러
/// API:
///   GET  /api/inquiry/my?memberId={}        - 내 문의 목록
///   POST /api/inquiry?memberId={}           - 문의 작성
class InquiryController extends ChangeNotifier {
  final FlutterSecureStorage _secureStorage = const FlutterSecureStorage();

  List<InquiryModel> _inquiryList = [];
  List<InquiryModel> get inquiryList => _inquiryList;

  bool _isLoading = false;
  bool get isLoading => _isLoading;

  String? _errorMessage;
  String? get errorMessage => _errorMessage;

  Future<String?> _getAccessToken() async =>
      await _secureStorage.read(key: "accessToken");

  Future<String?> _getMemberId() async =>
      await _secureStorage.read(key: "memberId");

  Future<String?> _getMid() async =>
      await _secureStorage.read(key: "mid");

  /// 내 문의 목록 조회
  Future<void> fetchMyInquiries() async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final String? accessToken = await _getAccessToken();
      final String? memberIdStr = await _getMemberId();

      if (accessToken == null || memberIdStr == null) {
        _errorMessage = '로그인이 필요합니다.';
        _inquiryList = [];
        return;
      }

      final url = Uri.parse(
          '${ApiConstants.springBaseUrl}/inquiry/my?memberId=$memberIdStr');
      final response = await http.get(url, headers: {
        'Authorization': 'Bearer $accessToken',
        'Content-Type': 'application/json',
      });

      if (response.statusCode == 200) {
        final List<dynamic> jsonList =
            jsonDecode(utf8.decode(response.bodyBytes));
        _inquiryList =
            jsonList.map((json) => InquiryModel.fromJson(json)).toList();
      } else {
        _errorMessage = '문의 목록 조회 실패 (${response.statusCode})';
        _inquiryList = [];
      }
    } catch (e) {
      _errorMessage = '네트워크 오류: $e';
      print('문의 목록 불러오기 에러: $e');
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  /// 문의 작성
  /// API: POST /api/inquiry?memberId={} + body: {title, content, writer, secret}
  Future<bool> postInquiry(InquiryModel inquiry) async {
    _isLoading = true;
    notifyListeners();

    bool isSuccess = false;
    try {
      final String? accessToken = await _getAccessToken();
      final String? memberIdStr = await _getMemberId();
      final String? mid = await _getMid();

      if (accessToken == null || memberIdStr == null) return false;

      final url = Uri.parse(
          '${ApiConstants.springBaseUrl}/inquiry?memberId=$memberIdStr');
      final response = await http.post(
        url,
        headers: {
          'Authorization': 'Bearer $accessToken',
          'Content-Type': 'application/json',
        },
        body: jsonEncode({
          'title': inquiry.title,
          'content': inquiry.content,
          'writer': mid ?? inquiry.writer ?? '익명',
          'secret': false,
        }),
      );

      if (response.statusCode == 201) {
        isSuccess = true;
        await fetchMyInquiries(); // 목록 갱신
      }
    } catch (e) {
      print('문의 작성 에러: $e');
    } finally {
      _isLoading = false;
      notifyListeners();
    }
    return isSuccess;
  }
}

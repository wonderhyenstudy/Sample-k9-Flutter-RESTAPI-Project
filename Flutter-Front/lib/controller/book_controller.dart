import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../model/book_model.dart';
import '../const/api_constants.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';

/// 도서 검색 및 상세 정보를 관리하는 상태 관리 컨트롤러 (Provider)
class BookController extends ChangeNotifier {
  final FlutterSecureStorage _secureStorage = const FlutterSecureStorage();

  List<BookModel> _bookList = [];
  List<BookModel> get bookList => _bookList;

  bool _isLoading = false;
  bool get isLoading => _isLoading;

  Future<String?> _getAccessToken() async {
    return await _secureStorage.read(key: "accessToken");
  }

  /// 도서 목록을 서버에서 불러오는 비동기 함수
  Future<void> fetchBooks() async {
    _isLoading = true;
    notifyListeners();

    try {
      final String? accessToken = await _getAccessToken();
      final url = Uri.parse('${ApiConstants.springBaseUrl}/book/list');
      final response = await http.get(url, headers: {
        if (accessToken != null) "Authorization": "Bearer $accessToken",
      });

      if (response.statusCode == 200) {
        final List<dynamic> jsonList = jsonDecode(response.body);
        _bookList = jsonList.map((json) => BookModel.fromJson(json)).toList();
      } else {
        _bookList = [];
      }
    } catch (e) {
      print('도서 불러오기 에러: $e');
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }
}

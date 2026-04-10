import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../model/book_model.dart';
import '../const/api_constants.dart';
import '../my_app.dart';
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

  Future<void> _handle401() async {
    await _secureStorage.deleteAll();
    MyApp.navigatorKey.currentState
        ?.pushNamedAndRemoveUntil('/login', (_) => false);
  }

  BookModel? _selectedBook;
  BookModel? get selectedBook => _selectedBook;

  /// 도서 목록 조회 (페이지)
  Future<void> fetchBooks({int page = 0, int size = 20}) async {
    _isLoading = true;
    notifyListeners();

    try {
      final String? accessToken = await _getAccessToken();
      final url =
          Uri.parse('${ApiConstants.springBaseUrl}/book?page=$page&size=$size');
      final response = await http.get(url, headers: {
        if (accessToken != null) 'Authorization': 'Bearer $accessToken',
        'Content-Type': 'application/json',
      });

      if (response.statusCode == 200) {
        final data = jsonDecode(utf8.decode(response.bodyBytes));
        // Page 응답과 List 응답 모두 처리
        final List<dynamic> jsonList =
            data is Map ? (data['content'] ?? []) : data as List;
        _bookList = jsonList.map((j) => BookModel.fromJson(j)).toList();
      } else if (response.statusCode == 401) {
        await _handle401();
      } else {
        _bookList = [];
      }
    } catch (e) {
      print('도서 목록 에러: $e');
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  /// 도서 상세 조회
  Future<void> fetchBookById(int bookId) async {
    _isLoading = true;
    _selectedBook = null;
    notifyListeners();

    try {
      final String? accessToken = await _getAccessToken();
      final url = Uri.parse('${ApiConstants.springBaseUrl}/book/$bookId');
      final response = await http.get(url, headers: {
        if (accessToken != null) 'Authorization': 'Bearer $accessToken',
        'Content-Type': 'application/json',
      });

      if (response.statusCode == 200) {
        _selectedBook =
            BookModel.fromJson(jsonDecode(utf8.decode(response.bodyBytes)));
      } else if (response.statusCode == 401) {
        await _handle401();
      }
    } catch (e) {
      print('도서 상세 에러: $e');
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }
}

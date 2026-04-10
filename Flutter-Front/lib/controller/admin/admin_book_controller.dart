import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:http/http.dart' as http;
import '../../const/api_constants.dart';
import '../../model/book_model.dart';

class AdminBookController extends ChangeNotifier {
  final _storage = const FlutterSecureStorage();
  List<BookModel> _books = [];
  List<BookModel> get books => _books;

  bool _isLoading = false;
  bool get isLoading => _isLoading;

  /// 도서의 현재 활성 대여 기록 조회 (GET /api/rental/active?bookId={id})
  /// 반납/연장 버튼에서 실제 rentalId를 얻기 위해 사용
  Future<Map<String, dynamic>?> getActiveRental(int bookId) async {
    try {
      final token = await _storage.read(key: 'accessToken');
      final res = await http.get(
        Uri.parse('${ApiConstants.springBaseUrl}/rental/active?bookId=$bookId'),
        headers: {'Authorization': 'Bearer $token'},
      );
      if (res.statusCode == 200) {
        return jsonDecode(utf8.decode(res.bodyBytes)) as Map<String, dynamic>;
      }
    } catch (_) {}
    return null;
  }

  /// 도서 반납 처리 (PUT /api/rental/{id}/return)
  Future<bool> returnBook(int rentalId) async {
    _isLoading = true;
    notifyListeners();
    try {
      final token = await _storage.read(key: 'accessToken');
      final res = await http.put(
        Uri.parse('${ApiConstants.springBaseUrl}/rental/$rentalId/return'),
        headers: {'Authorization': 'Bearer $token'},
      );
      return res.statusCode == 200;
    } catch (e) {
      return false;
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  /// 대여 기간 연장 (PUT /api/rental/{id}/extend)
  Future<bool> extendRental(int rentalId) async {
    _isLoading = true;
    notifyListeners();
    try {
      final token = await _storage.read(key: 'accessToken');
      final res = await http.put(
        Uri.parse('${ApiConstants.springBaseUrl}/rental/$rentalId/extend'),
        headers: {'Authorization': 'Bearer $token'},
      );
      return res.statusCode == 200;
    } catch (e) {
      return false;
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  /// 도서 목록 조회
  Future<void> fetchBooks() async {
    _isLoading = true;
    notifyListeners();
    try {
      final token = await _storage.read(key: 'accessToken');
      final res = await http.get(
        Uri.parse('${ApiConstants.springBaseUrl}/book?page=0&size=200'),
        headers: {'Authorization': 'Bearer $token'},
      );
      if (res.statusCode == 200) {
        final data = jsonDecode(utf8.decode(res.bodyBytes));
        final List<dynamic> content = data['content'] ?? [];
        _books = content.map((json) => BookModel.fromJson(json)).toList();
      }
    } catch (e) {
      print('Fetch Error: $e');
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  /// 도서 수정 (상태 변경 포함)
  Future<bool> updateBook(int id, BookModel book) async {
    try {
      final token = await _storage.read(key: 'accessToken');
      final res = await http.put(
        Uri.parse('${ApiConstants.springBaseUrl}/book/$id'),
        headers: {
          'Authorization': 'Bearer $token',
          'Content-Type': 'application/json',
        },
        body: jsonEncode(book.toJson()), // 모델의 status 값도 포함됨
      );
      if (res.statusCode == 200) {
        await fetchBooks(); // 목록 새로고침
        return true;
      }
    } catch (e) {
      print('Update Error: $e');
    }
    return false;
  }

  /// 도서 삭제
  Future<bool> deleteBook(int id) async {
    try {
      final token = await _storage.read(key: 'accessToken');
      final res = await http.delete(
        Uri.parse('${ApiConstants.springBaseUrl}/book/$id'),
        headers: {'Authorization': 'Bearer $token'},
      );
      if (res.statusCode == 200) {
        await fetchBooks();
        return true;
      }
    } catch (e) {
      print('Delete Error: $e');
    }
    return false;
  }
}
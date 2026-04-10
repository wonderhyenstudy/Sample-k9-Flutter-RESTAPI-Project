import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:http/http.dart' as http;

import '../../const/api_constants.dart';
import '../../model/book_model.dart';

/// 관리자 - 도서 관리 화면
/// GET /api/book?page=0&size=100
class AdminBookScreen extends StatefulWidget {
  const AdminBookScreen({super.key});

  @override
  State<AdminBookScreen> createState() => _AdminBookScreenState();
}

class _AdminBookScreenState extends State<AdminBookScreen> {
  List<dynamic> _books = [];
  bool _isLoading = true;
  String _searchQuery = '';

  @override
  void initState() {
    super.initState();
    _fetchBooks();
  }

  Future<void> _fetchBooks() async {
    setState(() => _isLoading = true);
    try {
      final token =
          await const FlutterSecureStorage().read(key: 'accessToken');
      final res = await http.get(
        Uri.parse('${ApiConstants.springBaseUrl}/book?page=0&size=200'),
        headers: {'Authorization': 'Bearer $token'},
      );
      if (res.statusCode == 200) {
        final data = jsonDecode(utf8.decode(res.bodyBytes));
        setState(() {
          _books = (data['content'] ?? data) as List<dynamic>;
        });
      }
    } catch (_) {
    } finally {
      setState(() => _isLoading = false);
    }
  }

  List<dynamic> get _filtered => _searchQuery.isEmpty
      ? _books
      : _books
          .where((b) =>
              (b['bookTitle'] ?? '').toLowerCase().contains(_searchQuery) ||
              (b['author'] ?? '').toLowerCase().contains(_searchQuery))
          .toList();

  Color _statusColor(String? status) {
    switch (status) {
      case 'AVAILABLE':
        return Colors.green;
      case 'RENTED':
        return Colors.orange;
      case 'RESERVED':
        return Colors.blue;
      default:
        return Colors.grey;
    }
  }

  String _statusLabel(String? status) {
    switch (status) {
      case 'AVAILABLE':
        return '대출가능';
      case 'RENTED':
        return '대출중';
      case 'RESERVED':
        return '예약됨';
      case 'LOST':
        return '분실';
      default:
        return status ?? '-';
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('도서 관리'),
        centerTitle: true,
        actions: [
          IconButton(
              icon: const Icon(Icons.refresh), onPressed: _fetchBooks),
        ],
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(12),
            child: TextField(
              decoration: const InputDecoration(
                hintText: '제목 또는 저자 검색',
                prefixIcon: Icon(Icons.search),
                border: OutlineInputBorder(),
                isDense: true,
              ),
              onChanged: (v) =>
                  setState(() => _searchQuery = v.toLowerCase()),
            ),
          ),
          Expanded(
            child: _isLoading
                ? const Center(child: CircularProgressIndicator())
                : _filtered.isEmpty
                    ? const Center(child: Text('도서가 없습니다.'))
                    : ListView.builder(
                        itemCount: _filtered.length,
                        itemBuilder: (_, i) {
                          final b = _filtered[i];
                          // JSON 맵 데이터를 BookModel로 변환하여 전달
                          final bookModel = BookModel.fromJson(b);
                          final status = bookModel.status;
                          return ListTile(
                            onTap: () {
                              // 상세 및 수정 화면으로 이동 후 목록 새로고침
                              Navigator.pushNamed(
                                context,
                                '/adminBookEdit',
                                arguments: bookModel,
                              ).then((_) => _fetchBooks());
                            },
                            leading: CircleAvatar(
                              backgroundColor:
                                  _statusColor(status).withOpacity(0.15),
                              child: Icon(Icons.menu_book,
                                  color: _statusColor(status)),
                            ),
                            title: Text(b['bookTitle'] ?? '-',
                                style: const TextStyle(
                                    fontWeight: FontWeight.w500)),
                            subtitle: Text(
                                '${b['author'] ?? '-'} · ${b['publisher'] ?? '-'}'),
                            trailing: Chip(
                              label: Text(_statusLabel(status),
                                  style: const TextStyle(
                                      fontSize: 11, color: Colors.white)),
                              backgroundColor: _statusColor(status),
                              padding: EdgeInsets.zero,
                            ),
                          );
                        },
                      ),
          ),
        ],
      ),
    );
  }
}

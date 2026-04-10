import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../controller/book_controller.dart';
import '../../model/book_model.dart';
import '../../widget/common/loading_widget.dart';
import '../../widget/common/empty_widget.dart';
import '../../widget/common/library_card_tile.dart';

/// 도서 탭
/// - 상단 검색바 (로컬 필터링)
/// - 도서 목록 ListView (카드형)
/// - 각 아이템 탭 → /bookDetail
class BookTab extends StatefulWidget {
  const BookTab({super.key});

  @override
  State<BookTab> createState() => _BookTabState();
}

class _BookTabState extends State<BookTab> {
  final TextEditingController _searchController = TextEditingController();
  String _query = '';

  @override
  void initState() {
    super.initState();
    Future.microtask(() {
      if (mounted) context.read<BookController>().fetchBooks();
    });
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  List<BookModel> _filtered(List<BookModel> list) {
    if (_query.isEmpty) return list;
    final q = _query.toLowerCase();
    return list
        .where((b) =>
            (b.title ?? '').toLowerCase().contains(q) ||
            (b.author ?? '').toLowerCase().contains(q) ||
            (b.publisher ?? '').toLowerCase().contains(q))
        .toList();
  }

  Color _statusColor(String? status) {
    switch (status) {
      case 'AVAILABLE':
        return Colors.green;
      case 'RENTED':
        return Colors.red;
      case 'RESERVED':
        return Colors.orange;
      default:
        return Colors.grey;
    }
  }

  String _statusLabel(String? status) {
    switch (status) {
      case 'AVAILABLE':
        return '대여가능';
      case 'RENTED':
        return '대여중';
      case 'RESERVED':
        return '예약중';
      case 'LOST':
        return '분실';
      default:
        return status ?? '-';
    }
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        // ── 검색바 ────────────────────────────────────
        Padding(
          padding: const EdgeInsets.fromLTRB(12, 12, 12, 4),
          child: TextField(
            controller: _searchController,
            onChanged: (v) => setState(() => _query = v),
            decoration: InputDecoration(
              hintText: '도서명, 저자, 출판사 검색',
              prefixIcon: const Icon(Icons.search),
              suffixIcon: _query.isNotEmpty
                  ? IconButton(
                      icon: const Icon(Icons.clear),
                      onPressed: () {
                        _searchController.clear();
                        setState(() => _query = '');
                      },
                    )
                  : null,
              border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12)),
              contentPadding:
                  const EdgeInsets.symmetric(vertical: 10, horizontal: 12),
            ),
          ),
        ),

        // ── 도서 목록 ─────────────────────────────────
        Expanded(
          child: Consumer<BookController>(
            builder: (_, ctrl, __) {
              if (ctrl.isLoading) {
                return const LoadingWidget(message: '도서 목록 불러오는 중...');
              }
              final books = _filtered(ctrl.bookList);
              if (books.isEmpty) {
                return EmptyWidget(
                  message: _query.isEmpty ? '등록된 도서가 없습니다.' : '"$_query" 검색 결과가 없습니다.',
                  icon: Icons.menu_book_outlined,
                );
              }
              return RefreshIndicator(
                onRefresh: () => context.read<BookController>().fetchBooks(),
                child: ListView.builder(
                  padding: const EdgeInsets.only(bottom: 16),
                  itemCount: books.length,
                  itemBuilder: (_, i) {
                    final book = books[i];
                    final statusLabel = _statusLabel(book.status);
                    final statusColor = _statusColor(book.status);
                    return LibraryCardTile(
                      leadingIcon: Icons.menu_book,
                      iconColor: statusColor,
                      title: book.title ?? '제목 없음',
                      subtitle: '${book.author ?? '저자 미상'}  |  ${book.publisher ?? ''}',
                      trailingWidget: Chip(
                        label: Text(statusLabel,
                            style: const TextStyle(
                                fontSize: 11, color: Colors.white)),
                        backgroundColor: statusColor,
                        padding: EdgeInsets.zero,
                        materialTapTargetSize:
                            MaterialTapTargetSize.shrinkWrap,
                      ),
                      onTap: () => Navigator.pushNamed(
                          context, '/bookDetail',
                          arguments: book.id),
                    );
                  },
                ),
              );
            },
          ),
        ),
      ],
    );
  }
}

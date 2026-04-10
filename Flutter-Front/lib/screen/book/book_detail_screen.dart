import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../controller/book_controller.dart';
import '../../controller/rental_controller.dart';

/// 도서 상세 정보 화면
/// Route arguments: int bookId
class BookDetailScreen extends StatefulWidget {
  const BookDetailScreen({super.key});

  @override
  State<BookDetailScreen> createState() => _BookDetailScreenState();
}

class _BookDetailScreenState extends State<BookDetailScreen> {
  int? _bookId;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final args = ModalRoute.of(context)?.settings.arguments;
    if (args is int && args != _bookId) {
      _bookId = args;
      Future.microtask(() {
        if (context.mounted) {
          context.read<BookController>().fetchBookById(_bookId!);
        }
      });
    }
  }

  Future<void> _rentBook() async {
    if (_bookId == null) return;

    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('대여 신청'),
        content: const Text('이 도서를 대여 신청하시겠습니까?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('취소'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('신청', style: TextStyle(color: Colors.blue)),
          ),
        ],
      ),
    );

    if (confirmed != true || !context.mounted) return;

    final success =
        await context.read<RentalController>().rentBook(_bookId!);

    if (!context.mounted) return;

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(success ? '대여 신청이 완료되었습니다.' : '대여 신청에 실패했습니다. 다시 시도해주세요.'),
      ),
    );

    if (success) {
      // 상태 갱신을 위해 도서 정보 다시 로드
      context.read<BookController>().fetchBookById(_bookId!);
    }
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
        return '대여 가능';
      case 'RENTED':
        return '대여 중';
      case 'RESERVED':
        return '예약 중';
      case 'LOST':
        return '분실';
      default:
        return status ?? '알 수 없음';
    }
  }

  @override
  Widget build(BuildContext context) {
    final bookCtrl = context.watch<BookController>();
    final rentalCtrl = context.watch<RentalController>();
    final book = bookCtrl.selectedBook;
    final isAvailable = book?.status == 'AVAILABLE';

    return Scaffold(
      appBar: AppBar(title: const Text('도서 상세 정보')),
      body: bookCtrl.isLoading
          ? const Center(child: CircularProgressIndicator())
          : book == null
              ? const Center(child: Text('도서 정보를 불러올 수 없습니다.'))
              : Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      // 도서 표지 플레이스홀더
                      Container(
                        height: 180,
                        width: double.infinity,
                        decoration: BoxDecoration(
                          color: Colors.grey[200],
                          borderRadius: BorderRadius.circular(8),
                        ),
                        child: const Center(
                          child: Icon(Icons.menu_book,
                              size: 64, color: Colors.grey),
                        ),
                      ),
                      const SizedBox(height: 20),

                      // 대여 상태 뱃지
                      Chip(
                        label: Text(
                          _statusLabel(book.status),
                          style: const TextStyle(color: Colors.white),
                        ),
                        backgroundColor: _statusColor(book.status),
                      ),
                      const SizedBox(height: 12),

                      // 도서 정보
                      _infoRow('도서명', book.title),
                      _infoRow('저자', book.author),
                      _infoRow('출판사', book.publisher),
                      _infoRow('ISBN', book.isbn),

                      const Spacer(),

                      // 대여 신청 버튼
                      SizedBox(
                        width: double.infinity,
                        height: 50,
                        child: ElevatedButton.icon(
                          onPressed: (isAvailable && !rentalCtrl.isLoading)
                              ? _rentBook
                              : null,
                          icon: rentalCtrl.isLoading
                              ? const SizedBox(
                                  width: 18,
                                  height: 18,
                                  child: CircularProgressIndicator(
                                      strokeWidth: 2, color: Colors.white),
                                )
                              : const Icon(Icons.library_add),
                          label: Text(
                            isAvailable ? '대여 신청' : _statusLabel(book.status),
                            style: const TextStyle(fontSize: 16),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
    );
  }

  Widget _infoRow(String label, String? value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 60,
            child: Text(label,
                style: const TextStyle(
                    fontWeight: FontWeight.bold, color: Colors.grey)),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Text(value ?? '-', style: const TextStyle(fontSize: 15)),
          ),
        ],
      ),
    );
  }
}

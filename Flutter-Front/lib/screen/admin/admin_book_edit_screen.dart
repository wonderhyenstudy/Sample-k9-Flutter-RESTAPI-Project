import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../model/book_model.dart';
import '../../controller/admin/admin_book_controller.dart';

class AdminBookEditScreen extends StatefulWidget {
  const AdminBookEditScreen({super.key}); // 생성자에서 book 제거

  @override
  State<AdminBookEditScreen> createState() => _AdminBookEditScreenState();
}

class _AdminBookEditScreenState extends State<AdminBookEditScreen> {
  late TextEditingController _titleController;
  late TextEditingController _authorController;
  late TextEditingController _pubController;
  late TextEditingController _descController;
  String? _selectedStatus;

  BookModel? _book; // 인자로 받을 책 객체 저장
  bool _isInit = false; // 초기화 여부 확인용

  // 현재 활성 대여 기록 (bookId로 조회)
  Map<String, dynamic>? _activeRental;
  bool _rentalLoading = false;

  final List<Map<String, String>> _statusOptions = [
    {'value': 'AVAILABLE', 'label': '대출가능'},
    {'value': 'RENTED', 'label': '대출중'},
    {'value': 'RESERVED', 'label': '예약됨'},
    {'value': 'LOST', 'label': '분실'},
  ];

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    // ── 라우트 arguments 꺼내기 ──
    if (!_isInit) {
      final args = ModalRoute.of(context)?.settings.arguments;
      if (args is BookModel) {
        _book = args;
        _titleController = TextEditingController(text: _book!.title);
        _authorController = TextEditingController(text: _book!.author);
        _pubController = TextEditingController(text: _book!.publisher);
        _descController = TextEditingController(text: _book!.description);
        _selectedStatus = _book!.status;
        // 대출중이면 활성 대여 기록 조회 (실제 rentalId 확보)
        if (_book!.status == 'RENTED' || _book!.status == 'EXTENDED') {
          _loadActiveRental();
        }
      }
      _isInit = true; // 중복 초기화 방지
    }
  }

  Future<void> _loadActiveRental() async {
    if (_book?.id == null) return;
    setState(() => _rentalLoading = true);
    final rental = await context
        .read<AdminBookController>()
        .getActiveRental(_book!.id!);
    if (mounted) {
      setState(() {
        _activeRental = rental;
        _rentalLoading = false;
      });
    }
  }

  // 삭제 확인 팝업
  void _confirmDelete() {
    if (_book?.id == null) return;
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('도서 삭제'),
        content: const Text('정말 이 도서를 삭제하시겠습니까?\n대여 중인 도서는 삭제할 수 없습니다.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('취소')),
          TextButton(
            onPressed: () async {
              final ctrl = context.read<AdminBookController>();
              final dialogNav = Navigator.of(ctx);
              final nav = Navigator.of(context);
              final success = await ctrl.deleteBook(_book!.id!);
              if (mounted) {
                dialogNav.pop();
                if (success) nav.pop();
              }
            },
            child: const Text('삭제', style: TextStyle(color: Colors.red)),
          ),
        ],
      ),
    );
  }

  // 수정 저장
  void _save() async {
    if (_book == null) return;

    final updatedBook = BookModel(
      id: _book!.id,
      title: _titleController.text,
      author: _authorController.text,
      publisher: _pubController.text,
      description: _descController.text,
      status: _selectedStatus,
      isbn: _book!.isbn,
    );

    final success = await context.read<AdminBookController>().updateBook(_book!.id!, updatedBook);
    if (success && mounted) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('수정되었습니다.')));
      Navigator.pop(context);
    }
  }

  // 반납 처리 함수
  void _handleReturn(int rentalId) async {
    final success = await context.read<AdminBookController>().returnBook(rentalId);
    if (success && mounted) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('반납 처리가 완료되었습니다.')));
      // 도서 목록과 상세 정보를 새로고침하기 위해 뒤로 가기
      context.read<AdminBookController>().fetchBooks();
      Navigator.pop(context);
    }
  }

  // 연장 처리 함수
  void _handleExtend(int rentalId) async {
    final success = await context.read<AdminBookController>().extendRental(rentalId);
    if (success && mounted) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('7일 연장되었습니다.')));
      Navigator.pop(context);
    }
  }

  @override
  Widget build(BuildContext context) {
    // 데이터 로딩 전 방어 코드
    if (_book == null) {
      return const Scaffold(body: Center(child: Text('도서 정보를 찾을 수 없습니다.')));
    }
    final isRented = _selectedStatus == 'RENTED' || _selectedStatus == 'EXTENDED';

    return Scaffold(
      appBar: AppBar(
        title: const Text('도서 정보 관리'),
        actions: [
          IconButton(onPressed: _confirmDelete, icon: const Icon(Icons.delete_forever, color: Colors.red)),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          // ── [신규] 대여 관리 섹션 (대출중일 때만 표시) ──
          if (isRented) ...[
            _buildAdminRentalSection(),
            const SizedBox(height: 24),
            const Divider(thickness: 2),
            const SizedBox(height: 24),
          ],

          const Text('도서 기본 정보 수정', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
          const SizedBox(height: 16),

          const Text('도서 상태', style: TextStyle(fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          DropdownButtonFormField<String>(
            value: _selectedStatus,
            decoration: const InputDecoration(border: OutlineInputBorder()),
            items: _statusOptions.map((opt) {
              return DropdownMenuItem(value: opt['value'], child: Text(opt['label']!));
            }).toList(),
            onChanged: (val) => setState(() => _selectedStatus = val),
          ),
          const SizedBox(height: 20),
          _buildTextField('도서명', _titleController),
          _buildTextField('저자', _authorController),
          _buildTextField('출판사', _pubController),
          _buildTextField('도서 설명', _descController, maxLines: 5),
          const SizedBox(height: 30),
          SizedBox(
            height: 50,
            child: ElevatedButton(
              onPressed: _save,
              child: const Text('설정 저장하기', style: TextStyle(fontSize: 16)),
            ),
          ),
        ],
      ),
    );
  }

  // 관리자 전용 반납/연장 제어 UI
  Widget _buildAdminRentalSection() {
    // 대여 기록 로딩 중
    if (_rentalLoading) {
      return Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: Colors.orange.withValues(alpha: 0.1),
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: Colors.orange),
        ),
        child: const Center(child: CircularProgressIndicator()),
      );
    }

    // 활성 대여 기록의 실제 rentalId
    final rentalId = _activeRental?['id'] as int?;

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.orange.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: Colors.orange),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Row(
            children: [
              Icon(Icons.admin_panel_settings, color: Colors.orange),
              SizedBox(width: 8),
              Text('현재 대여 관리',
                  style: TextStyle(
                      fontWeight: FontWeight.bold,
                      fontSize: 16,
                      color: Colors.orange)),
            ],
          ),
          const SizedBox(height: 12),
          // 대여자 정보 표시
          if (_activeRental != null) ...[
            _infoRow('대여자',
                '${_activeRental!['memberName'] ?? '-'} (${_activeRental!['memberMid'] ?? '-'})'),
            _infoRow('대여일', _activeRental!['rentalDate']?.toString() ?? '-'),
            _infoRow('반납기한', _activeRental!['dueDate']?.toString() ?? '-'),
            if (_activeRental!['overdue'] == true)
              const Padding(
                padding: EdgeInsets.only(top: 4),
                child: Text('⚠️ 연체 중',
                    style: TextStyle(
                        color: Colors.red, fontWeight: FontWeight.bold)),
              ),
            const SizedBox(height: 12),
          ] else ...[
            const Text('대여 정보를 불러올 수 없습니다.',
                style: TextStyle(fontSize: 13, color: Colors.grey)),
            const SizedBox(height: 12),
          ],
          Row(
            children: [
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: rentalId == null
                      ? null
                      : () => _handleExtend(rentalId),
                  icon: const Icon(Icons.history),
                  label: const Text('기한 연장'),
                  style:
                      OutlinedButton.styleFrom(foregroundColor: Colors.blue),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: ElevatedButton.icon(
                  onPressed: rentalId == null
                      ? null
                      : () => _handleReturn(rentalId),
                  icon: const Icon(Icons.assignment_return),
                  label: const Text('강제 반납'),
                  style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.orange,
                      foregroundColor: Colors.white),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _infoRow(String label, String value) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 2),
        child: Row(
          children: [
            SizedBox(
                width: 65,
                child: Text(label,
                    style: TextStyle(color: Colors.grey[600], fontSize: 12))),
            Expanded(
                child: Text(value,
                    style: const TextStyle(
                        fontWeight: FontWeight.w500, fontSize: 13))),
          ],
        ),
      );

  Widget _buildTextField(String label, TextEditingController controller, {int maxLines = 1}) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 16),
      child: TextField(
        controller: controller,
        maxLines: maxLines,
        decoration: InputDecoration(
          labelText: label,
          border: const OutlineInputBorder(),
          alignLabelWithHint: true,
        ),
      ),
    );
  }

  @override
  void dispose() {
    _titleController.dispose();
    _authorController.dispose();
    _pubController.dispose();
    _descController.dispose();
    super.dispose();
  }
}
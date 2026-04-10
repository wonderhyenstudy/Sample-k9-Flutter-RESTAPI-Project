import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:http/http.dart' as http;

import '../../const/api_constants.dart';

/// 관리자 - 이벤트 관리 화면
///
/// 제공 기능:
///  - 이벤트 목록 조회 : GET    /api/event?page=0&size=100
///  - 이벤트 등록      : POST   /api/event
///  - 이벤트 수정      : PUT    /api/event/{id}
///  - 이벤트 삭제      : DELETE /api/event/{id}
///
/// 서버 필드: category, title, content, eventDate(yyyy-MM-dd),
///           place, maxParticipants, status(OPEN/CLOSED)
class AdminEventScreen extends StatefulWidget {
  const AdminEventScreen({super.key});

  @override
  State<AdminEventScreen> createState() => _AdminEventScreenState();
}

class _AdminEventScreenState extends State<AdminEventScreen> {
  final _storage = const FlutterSecureStorage();
  List<dynamic> _events = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _fetchEvents();
  }

  // ─────────────────────────────────────────────
  // API 호출
  // ─────────────────────────────────────────────

  Future<void> _fetchEvents() async {
    setState(() => _isLoading = true);
    try {
      final token = await _storage.read(key: 'accessToken');
      final res = await http.get(
        Uri.parse('${ApiConstants.springBaseUrl}/event?page=0&size=100'),
        headers: {'Authorization': 'Bearer $token'},
      );
      if (res.statusCode == 200) {
        final data = jsonDecode(utf8.decode(res.bodyBytes));
        setState(() {
          _events = (data['content'] ?? data) as List<dynamic>;
        });
      }
    } catch (_) {
    } finally {
      setState(() => _isLoading = false);
    }
  }

  Future<bool> _createEvent(Map<String, dynamic> body) async {
    try {
      final token = await _storage.read(key: 'accessToken');
      final res = await http.post(
        Uri.parse('${ApiConstants.springBaseUrl}/event'),
        headers: {
          'Authorization': 'Bearer $token',
          'Content-Type': 'application/json',
        },
        body: jsonEncode(body),
      );
      return res.statusCode == 201 || res.statusCode == 200;
    } catch (_) {
      return false;
    }
  }

  Future<bool> _updateEvent(int id, Map<String, dynamic> body) async {
    try {
      final token = await _storage.read(key: 'accessToken');
      final res = await http.put(
        Uri.parse('${ApiConstants.springBaseUrl}/event/$id'),
        headers: {
          'Authorization': 'Bearer $token',
          'Content-Type': 'application/json',
        },
        body: jsonEncode(body),
      );
      return res.statusCode == 200;
    } catch (_) {
      return false;
    }
  }

  Future<bool> _deleteEvent(int id) async {
    try {
      final token = await _storage.read(key: 'accessToken');
      final res = await http.delete(
        Uri.parse('${ApiConstants.springBaseUrl}/event/$id'),
        headers: {'Authorization': 'Bearer $token'},
      );
      return res.statusCode == 200;
    } catch (_) {
      return false;
    }
  }

  // ─────────────────────────────────────────────
  // 폼 다이얼로그 (등록 / 수정)
  // ─────────────────────────────────────────────

  Future<void> _showFormDialog({Map<String, dynamic>? existing}) async {
    final isEdit = existing != null;
    final titleCtrl = TextEditingController(text: existing?['title'] ?? '');
    final contentCtrl =
        TextEditingController(text: existing?['content'] ?? '');
    final categoryCtrl =
        TextEditingController(text: existing?['category'] ?? '문화');
    final placeCtrl = TextEditingController(text: existing?['place'] ?? '');
    final maxCtrl = TextEditingController(
        text: (existing?['maxParticipants'] ?? 30).toString());
    DateTime selectedDate = DateTime.tryParse(
            existing?['eventDate']?.toString().split('T').first ?? '') ??
        DateTime.now().add(const Duration(days: 7));
    String status = (existing?['status'] ?? 'OPEN').toString();

    final saved = await showDialog<bool>(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setLocal) => AlertDialog(
          title: Text(isEdit ? '이벤트 수정' : '이벤트 등록'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  controller: titleCtrl,
                  decoration: const InputDecoration(labelText: '제목'),
                ),
                TextField(
                  controller: categoryCtrl,
                  decoration: const InputDecoration(
                      labelText: '카테고리 (문화/교육/체험 등)'),
                ),
                TextField(
                  controller: contentCtrl,
                  maxLines: 3,
                  decoration: const InputDecoration(labelText: '내용'),
                ),
                TextField(
                  controller: placeCtrl,
                  decoration: const InputDecoration(labelText: '장소'),
                ),
                TextField(
                  controller: maxCtrl,
                  keyboardType: TextInputType.number,
                  decoration:
                      const InputDecoration(labelText: '최대 인원'),
                ),
                const SizedBox(height: 8),
                Row(
                  children: [
                    const Text('행사일: '),
                    Text(
                      '${selectedDate.year}-${selectedDate.month.toString().padLeft(2, '0')}-${selectedDate.day.toString().padLeft(2, '0')}',
                      style: const TextStyle(fontWeight: FontWeight.bold),
                    ),
                    IconButton(
                      icon: const Icon(Icons.calendar_today),
                      onPressed: () async {
                        final picked = await showDatePicker(
                          context: ctx,
                          initialDate: selectedDate,
                          firstDate: DateTime(2020),
                          lastDate: DateTime(2100),
                        );
                        if (picked != null) {
                          setLocal(() => selectedDate = picked);
                        }
                      },
                    ),
                  ],
                ),
                if (isEdit)
                  DropdownButtonFormField<String>(
                    value: status,
                    decoration: const InputDecoration(labelText: '상태'),
                    items: const [
                      DropdownMenuItem(value: 'OPEN', child: Text('모집중')),
                      DropdownMenuItem(
                          value: 'CLOSED', child: Text('모집마감')),
                    ],
                    onChanged: (v) => setLocal(() => status = v ?? 'OPEN'),
                  ),
              ],
            ),
          ),
          actions: [
            TextButton(
                onPressed: () => Navigator.pop(ctx, false),
                child: const Text('취소')),
            ElevatedButton(
                onPressed: () => Navigator.pop(ctx, true),
                child: const Text('저장')),
          ],
        ),
      ),
    );

    if (saved == true) {
      final body = <String, dynamic>{
        'title': titleCtrl.text.trim(),
        'category': categoryCtrl.text.trim(),
        'content': contentCtrl.text.trim(),
        'place': placeCtrl.text.trim(),
        'maxParticipants': int.tryParse(maxCtrl.text.trim()) ?? 0,
        'eventDate':
            '${selectedDate.year}-${selectedDate.month.toString().padLeft(2, '0')}-${selectedDate.day.toString().padLeft(2, '0')}',
        if (isEdit) 'status': status,
      };
      final ok = isEdit
          ? await _updateEvent(existing!['id'] as int, body)
          : await _createEvent(body);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
            content: Text(ok
                ? (isEdit ? '수정 완료' : '등록 완료')
                : (isEdit ? '수정 실패' : '등록 실패'))),
      );
      if (ok) _fetchEvents();
    }
  }

  Future<void> _confirmDelete(Map<String, dynamic> e) async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('이벤트 삭제'),
        content: Text('"${e['title'] ?? ''}" 이벤트를 삭제할까요?'),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(ctx, false),
              child: const Text('취소')),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: Colors.red),
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('삭제'),
          ),
        ],
      ),
    );
    if (ok == true) {
      final success = await _deleteEvent(e['id'] as int);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(success ? '삭제 완료' : '삭제 실패')),
      );
      if (success) _fetchEvents();
    }
  }

  // ─────────────────────────────────────────────
  // UI
  // ─────────────────────────────────────────────

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('이벤트 관리 (총 ${_events.length}건)'),
        centerTitle: true,
        actions: [
          IconButton(
              icon: const Icon(Icons.refresh), onPressed: _fetchEvents),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => _showFormDialog(),
        icon: const Icon(Icons.add),
        label: const Text('이벤트 등록'),
        backgroundColor: Colors.orange,
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _events.isEmpty
              ? const Center(child: Text('등록된 이벤트가 없습니다.'))
              : ListView.builder(
                  padding: const EdgeInsets.all(12),
                  itemCount: _events.length,
                  itemBuilder: (_, i) {
                    final e = _events[i] as Map<String, dynamic>;
                    final date = e['eventDate'] ?? '-';
                    final status =
                        (e['status'] ?? 'OPEN').toString();
                    return Card(
                      margin: const EdgeInsets.only(bottom: 8),
                      child: ListTile(
                        leading: CircleAvatar(
                          backgroundColor: status == 'OPEN'
                              ? Colors.orange.withOpacity(0.15)
                              : Colors.grey.withOpacity(0.2),
                          child: Icon(Icons.celebration,
                              color: status == 'OPEN'
                                  ? Colors.orange
                                  : Colors.grey),
                        ),
                        title: Text(e['title'] ?? '-',
                            style: const TextStyle(
                                fontWeight: FontWeight.w500)),
                        subtitle: Text(
                            '일시: $date · 장소: ${e['place'] ?? '-'}\n인원: ${e['currentParticipants'] ?? 0}/${e['maxParticipants'] ?? 0} · 상태: $status'),
                        isThreeLine: true,
                        trailing: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            IconButton(
                              icon: const Icon(Icons.edit,
                                  color: Colors.blue),
                              onPressed: () =>
                                  _showFormDialog(existing: e),
                            ),
                            IconButton(
                              icon: const Icon(Icons.delete,
                                  color: Colors.red),
                              onPressed: () => _confirmDelete(e),
                            ),
                          ],
                        ),
                      ),
                    );
                  },
                ),
    );
  }
}

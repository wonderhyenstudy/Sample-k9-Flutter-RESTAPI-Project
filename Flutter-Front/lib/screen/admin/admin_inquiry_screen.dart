import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:http/http.dart' as http;

import '../../const/api_constants.dart';

/// 관리자 - 문의 관리 화면
///
/// 제공 기능:
///  - 문의 목록 조회 : GET    /api/inquiry?page=0&size=200   (viewerMemberId 없음 → 관리자)
///  - 상세 조회      : GET    /api/inquiry/{id}
///  - 문의 수정      : PUT    /api/inquiry/{id}
///  - 문의 삭제      : DELETE /api/inquiry/{id}
///  - 답변 작성      : POST   /api/inquiry/{id}/reply
class AdminInquiryScreen extends StatefulWidget {
  const AdminInquiryScreen({super.key});

  @override
  State<AdminInquiryScreen> createState() => _AdminInquiryScreenState();
}

class _AdminInquiryScreenState extends State<AdminInquiryScreen> {
  final _storage = const FlutterSecureStorage();
  List<dynamic> _inquiries = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _fetchInquiries();
  }

  // ─────────────────────────────────────────────
  // API 호출
  // ─────────────────────────────────────────────

  Future<void> _fetchInquiries() async {
    setState(() => _isLoading = true);
    try {
      final token = await _storage.read(key: 'accessToken');
      final res = await http.get(
        Uri.parse('${ApiConstants.springBaseUrl}/inquiry?page=0&size=200'),
        headers: {'Authorization': 'Bearer $token'},
      );
      if (res.statusCode == 200) {
        final data = jsonDecode(utf8.decode(res.bodyBytes));
        setState(() {
          _inquiries = (data['content'] ?? data) as List<dynamic>;
        });
      }
    } catch (_) {
    } finally {
      setState(() => _isLoading = false);
    }
  }

  Future<Map<String, dynamic>?> _fetchDetail(int id) async {
    try {
      final token = await _storage.read(key: 'accessToken');
      final res = await http.get(
        Uri.parse('${ApiConstants.springBaseUrl}/inquiry/$id'),
        headers: {'Authorization': 'Bearer $token'},
      );
      if (res.statusCode == 200) {
        return jsonDecode(utf8.decode(res.bodyBytes))
            as Map<String, dynamic>;
      }
    } catch (_) {}
    return null;
  }

  Future<bool> _updateInquiry(int id, Map<String, dynamic> body) async {
    try {
      final token = await _storage.read(key: 'accessToken');
      final res = await http.put(
        Uri.parse('${ApiConstants.springBaseUrl}/inquiry/$id'),
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

  Future<bool> _deleteInquiry(int id) async {
    try {
      final token = await _storage.read(key: 'accessToken');
      final res = await http.delete(
        Uri.parse('${ApiConstants.springBaseUrl}/inquiry/$id'),
        headers: {'Authorization': 'Bearer $token'},
      );
      return res.statusCode == 200;
    } catch (_) {
      return false;
    }
  }

  Future<bool> _addReply(
      int inquiryId, String replyText, String replier) async {
    try {
      final token = await _storage.read(key: 'accessToken');
      final res = await http.post(
        Uri.parse(
            '${ApiConstants.springBaseUrl}/inquiry/$inquiryId/reply'),
        headers: {
          'Authorization': 'Bearer $token',
          'Content-Type': 'application/json',
        },
        body: jsonEncode({
          'replyText': replyText,
          'replier': replier,
        }),
      );
      return res.statusCode == 201 || res.statusCode == 200;
    } catch (_) {
      return false;
    }
  }

  // ─────────────────────────────────────────────
  // 다이얼로그
  // ─────────────────────────────────────────────

  Future<void> _showEditDialog(Map<String, dynamic> inq) async {
    final detail = await _fetchDetail(inq['id'] as int) ?? inq;
    if (!mounted) return;

    final titleCtrl =
        TextEditingController(text: detail['title'] ?? '');
    final contentCtrl =
        TextEditingController(text: detail['content'] ?? '');
    bool secret = (detail['secret'] ?? false) as bool;

    final saved = await showDialog<bool>(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setLocal) => AlertDialog(
          title: const Text('문의사항 수정'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  controller: titleCtrl,
                  decoration: const InputDecoration(labelText: '제목'),
                ),
                TextField(
                  controller: contentCtrl,
                  maxLines: 5,
                  decoration: const InputDecoration(labelText: '내용'),
                ),
                SwitchListTile(
                  contentPadding: EdgeInsets.zero,
                  title: const Text('비밀글'),
                  value: secret,
                  onChanged: (v) => setLocal(() => secret = v),
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
      final ok = await _updateInquiry(inq['id'] as int, {
        'title': titleCtrl.text.trim(),
        'content': contentCtrl.text.trim(),
        'secret': secret,
      });
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(ok ? '수정 완료' : '수정 실패')),
      );
      if (ok) _fetchInquiries();
    }
  }

  Future<void> _showReplyDialog(Map<String, dynamic> inq) async {
    final detail = await _fetchDetail(inq['id'] as int) ?? inq;
    if (!mounted) return;

    final replyCtrl = TextEditingController();
    final replierCtrl = TextEditingController(text: '도서관 관리자');
    final replies = (detail['replies'] as List?) ?? const [];

    final saved = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('답변 작성'),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('Q. ${detail['title'] ?? ''}',
                  style: const TextStyle(fontWeight: FontWeight.bold)),
              const SizedBox(height: 4),
              Text(detail['content'] ?? '',
                  style: const TextStyle(color: Colors.black87)),
              const Divider(height: 20),
              if (replies.isNotEmpty) ...[
                const Text('기존 답변',
                    style: TextStyle(fontWeight: FontWeight.bold)),
                ...replies.map((r) => Padding(
                      padding: const EdgeInsets.symmetric(vertical: 4),
                      child: Text(
                          '- (${r['replier'] ?? ''}) ${r['replyText'] ?? ''}'),
                    )),
                const SizedBox(height: 8),
              ],
              TextField(
                controller: replierCtrl,
                decoration: const InputDecoration(labelText: '답변자'),
              ),
              TextField(
                controller: replyCtrl,
                maxLines: 4,
                decoration: const InputDecoration(labelText: '답변 내용'),
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
              child: const Text('답변 등록')),
        ],
      ),
    );

    if (saved == true && replyCtrl.text.trim().isNotEmpty) {
      final ok = await _addReply(
        inq['id'] as int,
        replyCtrl.text.trim(),
        replierCtrl.text.trim(),
      );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(ok ? '답변이 등록되었습니다' : '답변 등록 실패')),
      );
      if (ok) _fetchInquiries();
    }
  }

  Future<void> _confirmDelete(Map<String, dynamic> inq) async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('문의 삭제'),
        content: Text('"${inq['title'] ?? ''}" 문의사항과 관련 답변을 모두 삭제할까요?'),
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
      final success = await _deleteInquiry(inq['id'] as int);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(success ? '삭제 완료' : '삭제 실패')),
      );
      if (success) _fetchInquiries();
    }
  }

  // ─────────────────────────────────────────────
  // UI
  // ─────────────────────────────────────────────

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('문의 관리 (총 ${_inquiries.length}건)'),
        centerTitle: true,
        actions: [
          IconButton(
              icon: const Icon(Icons.refresh), onPressed: _fetchInquiries),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _inquiries.isEmpty
              ? const Center(child: Text('문의가 없습니다.'))
              : ListView.builder(
                  padding: const EdgeInsets.all(12),
                  itemCount: _inquiries.length,
                  itemBuilder: (_, i) {
                    final inq = _inquiries[i] as Map<String, dynamic>;
                    final answered = (inq['answered'] ?? false) as bool;
                    final secret = (inq['secret'] ?? false) as bool;
                    return Card(
                      margin: const EdgeInsets.only(bottom: 8),
                      child: ListTile(
                        leading: CircleAvatar(
                          backgroundColor: answered
                              ? Colors.green.withOpacity(0.15)
                              : Colors.red.withOpacity(0.15),
                          child: Icon(
                            answered
                                ? Icons.check_circle
                                : Icons.pending_outlined,
                            color:
                                answered ? Colors.green : Colors.red,
                          ),
                        ),
                        title: Row(
                          children: [
                            if (secret)
                              const Padding(
                                padding: EdgeInsets.only(right: 4),
                                child: Icon(Icons.lock,
                                    size: 14, color: Colors.grey),
                              ),
                            Flexible(
                              child: Text(inq['title'] ?? '-',
                                  overflow: TextOverflow.ellipsis,
                                  style: const TextStyle(
                                      fontWeight: FontWeight.w500)),
                            ),
                          ],
                        ),
                        subtitle: Text(
                            '작성자: ${inq['writer'] ?? '-'}  · ${answered ? '답변완료' : '미답변'}'),
                        trailing: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            IconButton(
                              icon: const Icon(Icons.reply,
                                  color: Colors.green),
                              tooltip: '답변',
                              onPressed: () => _showReplyDialog(inq),
                            ),
                            IconButton(
                              icon: const Icon(Icons.edit,
                                  color: Colors.blue),
                              tooltip: '수정',
                              onPressed: () => _showEditDialog(inq),
                            ),
                            IconButton(
                              icon: const Icon(Icons.delete,
                                  color: Colors.red),
                              tooltip: '삭제',
                              onPressed: () => _confirmDelete(inq),
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

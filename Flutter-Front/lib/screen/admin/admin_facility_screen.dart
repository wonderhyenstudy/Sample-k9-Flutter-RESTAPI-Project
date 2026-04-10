import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:http/http.dart' as http;

import '../../const/api_constants.dart';

/// 관리자 - 시설예약 관리 화면
///
/// 제공 기능:
///  - 전체 예약 조회 : GET    /api/apply?page=0&size=200
///  - 관리자 등록    : POST   /api/apply/admin?memberId={id}
///  - 예약 정보 수정 : PUT    /api/apply/{id}
///  - 예약 삭제      : DELETE /api/apply/{id}
///  - 승인 / 반려    : PUT    /api/apply/{id}/approve|/reject
///
/// 서버 필드: applicantName, facilityType, phone, participants,
///           reserveDate(yyyy-MM-dd), status(PENDING/APPROVED/REJECTED)
class AdminFacilityScreen extends StatefulWidget {
  const AdminFacilityScreen({super.key});

  @override
  State<AdminFacilityScreen> createState() => _AdminFacilityScreenState();
}

class _AdminFacilityScreenState extends State<AdminFacilityScreen> {
  final _storage = const FlutterSecureStorage();
  List<dynamic> _applies = [];
  bool _isLoading = true;

  static const _facilityTypes = ['세미나실', '스터디룸', '강당'];

  @override
  void initState() {
    super.initState();
    _fetchApplies();
  }

  // ─────────────────────────────────────────────
  // API 호출
  // ─────────────────────────────────────────────

  Future<void> _fetchApplies() async {
    setState(() => _isLoading = true);
    try {
      final token = await _storage.read(key: 'accessToken');
      final res = await http.get(
        Uri.parse('${ApiConstants.springBaseUrl}/apply?page=0&size=200'),
        headers: {'Authorization': 'Bearer $token'},
      );
      if (res.statusCode == 200) {
        final data = jsonDecode(utf8.decode(res.bodyBytes));
        setState(() {
          _applies = (data['content'] ?? data) as List<dynamic>;
        });
      }
    } catch (_) {
    } finally {
      setState(() => _isLoading = false);
    }
  }

  Future<bool> _createApplyAsAdmin(
      int memberId, Map<String, dynamic> body) async {
    try {
      final token = await _storage.read(key: 'accessToken');
      final res = await http.post(
        Uri.parse(
            '${ApiConstants.springBaseUrl}/apply/admin?memberId=$memberId'),
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

  Future<bool> _updateApply(int id, Map<String, dynamic> body) async {
    try {
      final token = await _storage.read(key: 'accessToken');
      final res = await http.put(
        Uri.parse('${ApiConstants.springBaseUrl}/apply/$id'),
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

  Future<bool> _deleteApply(int id) async {
    try {
      final token = await _storage.read(key: 'accessToken');
      final res = await http.delete(
        Uri.parse('${ApiConstants.springBaseUrl}/apply/$id'),
        headers: {'Authorization': 'Bearer $token'},
      );
      return res.statusCode == 200;
    } catch (_) {
      return false;
    }
  }

  Future<bool> _changeStatus(int id, String action) async {
    try {
      final token = await _storage.read(key: 'accessToken');
      final res = await http.put(
        Uri.parse('${ApiConstants.springBaseUrl}/apply/$id/$action'),
        headers: {'Authorization': 'Bearer $token'},
      );
      return res.statusCode == 200;
    } catch (_) {
      return false;
    }
  }

  // ─────────────────────────────────────────────
  // 다이얼로그
  // ─────────────────────────────────────────────

  Future<void> _showFormDialog({Map<String, dynamic>? existing}) async {
    final isEdit = existing != null;
    final memberIdCtrl = TextEditingController(
        text: (existing?['memberId'] ?? '').toString());
    final applicantCtrl =
        TextEditingController(text: existing?['applicantName'] ?? '');
    final phoneCtrl =
        TextEditingController(text: existing?['phone'] ?? '');
    final participantsCtrl = TextEditingController(
        text: (existing?['participants'] ?? 1).toString());
    String facility =
        (existing?['facilityType'] ?? _facilityTypes.first) as String;
    DateTime reserveDate = DateTime.tryParse(
            existing?['reserveDate']?.toString().split('T').first ?? '') ??
        DateTime.now().add(const Duration(days: 3));
    String status = (existing?['status'] ?? 'PENDING') as String;

    final saved = await showDialog<bool>(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setLocal) => AlertDialog(
          title: Text(isEdit ? '예약 수정' : '예약 등록 (관리자)'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  controller: memberIdCtrl,
                  enabled: !isEdit,
                  keyboardType: TextInputType.number,
                  decoration: InputDecoration(
                    labelText: '회원 ID',
                    helperText: isEdit ? '수정 시에는 변경할 수 없습니다' : null,
                  ),
                ),
                TextField(
                  controller: applicantCtrl,
                  decoration: const InputDecoration(labelText: '신청자 이름'),
                ),
                DropdownButtonFormField<String>(
                  value: facility,
                  decoration: const InputDecoration(labelText: '시설 유형'),
                  items: _facilityTypes
                      .map((f) => DropdownMenuItem(
                            value: f,
                            child: Text(f),
                          ))
                      .toList(),
                  onChanged: (v) =>
                      setLocal(() => facility = v ?? facility),
                ),
                TextField(
                  controller: phoneCtrl,
                  decoration:
                      const InputDecoration(labelText: '연락처 (010-...)'),
                ),
                TextField(
                  controller: participantsCtrl,
                  keyboardType: TextInputType.number,
                  decoration: const InputDecoration(labelText: '사용 인원'),
                ),
                const SizedBox(height: 8),
                Row(
                  children: [
                    const Text('예약일: '),
                    Text(
                      '${reserveDate.year}-${reserveDate.month.toString().padLeft(2, '0')}-${reserveDate.day.toString().padLeft(2, '0')}',
                      style: const TextStyle(fontWeight: FontWeight.bold),
                    ),
                    IconButton(
                      icon: const Icon(Icons.calendar_today),
                      onPressed: () async {
                        final picked = await showDatePicker(
                          context: ctx,
                          initialDate: reserveDate,
                          firstDate: DateTime(2020),
                          lastDate: DateTime(2100),
                        );
                        if (picked != null) {
                          setLocal(() => reserveDate = picked);
                        }
                      },
                    ),
                  ],
                ),
                DropdownButtonFormField<String>(
                  value: status,
                  decoration: const InputDecoration(labelText: '상태'),
                  items: const [
                    DropdownMenuItem(
                        value: 'PENDING', child: Text('대기')),
                    DropdownMenuItem(
                        value: 'APPROVED', child: Text('승인')),
                    DropdownMenuItem(
                        value: 'REJECTED', child: Text('반려')),
                  ],
                  onChanged: (v) =>
                      setLocal(() => status = v ?? 'PENDING'),
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
        'applicantName': applicantCtrl.text.trim(),
        'facilityType': facility,
        'phone': phoneCtrl.text.trim(),
        'participants': int.tryParse(participantsCtrl.text.trim()) ?? 1,
        'reserveDate':
            '${reserveDate.year}-${reserveDate.month.toString().padLeft(2, '0')}-${reserveDate.day.toString().padLeft(2, '0')}',
        'status': status,
      };
      bool ok;
      if (isEdit) {
        ok = await _updateApply(existing!['id'] as int, body);
      } else {
        final memberId = int.tryParse(memberIdCtrl.text.trim());
        if (memberId == null) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('회원 ID는 숫자여야 합니다')),
          );
          return;
        }
        ok = await _createApplyAsAdmin(memberId, body);
      }
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
            content: Text(ok
                ? (isEdit ? '수정 완료' : '등록 완료')
                : (isEdit ? '수정 실패' : '등록 실패'))),
      );
      if (ok) _fetchApplies();
    }
  }

  Future<void> _confirmDelete(Map<String, dynamic> a) async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('예약 삭제'),
        content: Text(
            '${a['applicantName'] ?? ''} 신청(${a['reserveDate'] ?? ''})을 삭제할까요?'),
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
      final success = await _deleteApply(a['id'] as int);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(success ? '삭제 완료' : '삭제 실패')),
      );
      if (success) _fetchApplies();
    }
  }

  Future<void> _handleStatusAction(
      Map<String, dynamic> a, String action, String label) async {
    final ok = await _changeStatus(a['id'] as int, action);
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(ok ? '$label 완료' : '$label 실패')),
    );
    if (ok) _fetchApplies();
  }

  // ─────────────────────────────────────────────
  // UI
  // ─────────────────────────────────────────────

  Color _statusColor(String status) {
    switch (status) {
      case 'APPROVED':
        return Colors.green;
      case 'REJECTED':
        return Colors.red;
      default:
        return Colors.orange;
    }
  }

  String _statusLabel(String status) {
    switch (status) {
      case 'APPROVED':
        return '승인';
      case 'REJECTED':
        return '반려';
      default:
        return '대기';
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('시설예약 관리 (총 ${_applies.length}건)'),
        centerTitle: true,
        actions: [
          IconButton(
              icon: const Icon(Icons.refresh), onPressed: _fetchApplies),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => _showFormDialog(),
        icon: const Icon(Icons.add),
        label: const Text('예약 등록'),
        backgroundColor: Colors.teal,
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _applies.isEmpty
              ? const Center(child: Text('시설 예약 내역이 없습니다.'))
              : ListView.builder(
                  padding: const EdgeInsets.all(12),
                  itemCount: _applies.length,
                  itemBuilder: (_, i) {
                    final a = _applies[i] as Map<String, dynamic>;
                    final status = (a['status'] ?? 'PENDING') as String;
                    final isPending = status == 'PENDING';
                    return Card(
                      margin: const EdgeInsets.only(bottom: 8),
                      child: Padding(
                        padding: const EdgeInsets.all(12),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              children: [
                                CircleAvatar(
                                  backgroundColor: _statusColor(status)
                                      .withOpacity(0.15),
                                  child: Icon(
                                      Icons.meeting_room_outlined,
                                      color: _statusColor(status)),
                                ),
                                const SizedBox(width: 12),
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment:
                                        CrossAxisAlignment.start,
                                    children: [
                                      Text(
                                        '${a['facilityType'] ?? '-'}  ·  ${a['applicantName'] ?? '-'}',
                                        style: const TextStyle(
                                            fontWeight:
                                                FontWeight.w600),
                                      ),
                                      const SizedBox(height: 2),
                                      Text(
                                          '예약일: ${a['reserveDate'] ?? '-'}  ·  인원: ${a['participants'] ?? 0}'),
                                      Text(
                                          '연락처: ${a['phone'] ?? '-'}  ·  회원: ${a['memberName'] ?? a['memberId'] ?? '-'}'),
                                    ],
                                  ),
                                ),
                                Chip(
                                  label: Text(
                                    _statusLabel(status),
                                    style: const TextStyle(
                                        color: Colors.white,
                                        fontSize: 11),
                                  ),
                                  backgroundColor:
                                      _statusColor(status),
                                  padding: EdgeInsets.zero,
                                ),
                              ],
                            ),
                            const SizedBox(height: 6),
                            Row(
                              mainAxisAlignment: MainAxisAlignment.end,
                              children: [
                                if (isPending) ...[
                                  TextButton.icon(
                                    icon: const Icon(Icons.check,
                                        color: Colors.green),
                                    label: const Text('승인'),
                                    onPressed: () =>
                                        _handleStatusAction(
                                            a, 'approve', '승인'),
                                  ),
                                  TextButton.icon(
                                    icon: const Icon(Icons.block,
                                        color: Colors.red),
                                    label: const Text('반려'),
                                    onPressed: () =>
                                        _handleStatusAction(
                                            a, 'reject', '반려'),
                                  ),
                                ],
                                IconButton(
                                  icon: const Icon(Icons.edit,
                                      color: Colors.blue),
                                  onPressed: () =>
                                      _showFormDialog(existing: a),
                                ),
                                IconButton(
                                  icon: const Icon(Icons.delete,
                                      color: Colors.red),
                                  onPressed: () => _confirmDelete(a),
                                ),
                              ],
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

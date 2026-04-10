import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:http/http.dart' as http;

import '../../const/api_constants.dart';

/// 관리자 - 회원 관리 화면
///
/// 제공 기능:
///  - 회원 목록 조회 : GET    /api/member/list
///  - 회원 정보 수정 : PUT    /api/member/admin/{id}
///  - 회원 삭제      : DELETE /api/member/{id}
///
/// 검색은 아이디/이름/이메일에 대해 대소문자 구분 없이 수행합니다.
class AdminMemberScreen extends StatefulWidget {
  const AdminMemberScreen({super.key});

  @override
  State<AdminMemberScreen> createState() => _AdminMemberScreenState();
}

class _AdminMemberScreenState extends State<AdminMemberScreen> {
  final _storage = const FlutterSecureStorage();
  List<dynamic> _members = [];
  bool _isLoading = true;
  String _searchQuery = '';

  @override
  void initState() {
    super.initState();
    _fetchMembers();
  }

  // ─────────────────────────────────────────────
  // 데이터 조회 / 수정 / 삭제
  // ─────────────────────────────────────────────

  Future<void> _fetchMembers() async {
    setState(() => _isLoading = true);
    try {
      final token = await _storage.read(key: 'accessToken');
      final res = await http.get(
        Uri.parse('${ApiConstants.springBaseUrl}/member/list'),
        headers: {'Authorization': 'Bearer $token'},
      );
      if (res.statusCode == 200) {
        setState(() {
          _members = jsonDecode(utf8.decode(res.bodyBytes)) as List<dynamic>;
        });
      }
    } catch (_) {
    } finally {
      setState(() => _isLoading = false);
    }
  }

  /// 회원 정보 수정 (관리자 전용)
  /// 백엔드: PUT /api/member/admin/{id}
  Future<bool> _updateMember(int id, Map<String, dynamic> body) async {
    try {
      final token = await _storage.read(key: 'accessToken');
      final res = await http.put(
        Uri.parse('${ApiConstants.springBaseUrl}/member/admin/$id'),
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

  /// 회원 삭제 (관리자 전용)
  /// 백엔드: DELETE /api/member/{id}
  Future<bool> _deleteMember(int id) async {
    try {
      final token = await _storage.read(key: 'accessToken');
      final res = await http.delete(
        Uri.parse('${ApiConstants.springBaseUrl}/member/$id'),
        headers: {'Authorization': 'Bearer $token'},
      );
      return res.statusCode == 200;
    } catch (_) {
      return false;
    }
  }

  List<dynamic> get _filtered => _searchQuery.isEmpty
      ? _members
      : _members
          .where((m) =>
              (m['mid'] ?? '').toString().toLowerCase().contains(_searchQuery) ||
              (m['mname'] ?? '').toString().toLowerCase().contains(_searchQuery) ||
              (m['email'] ?? '').toString().toLowerCase().contains(_searchQuery))
          .toList();

  // ─────────────────────────────────────────────
  // 다이얼로그 (수정 / 삭제 확인)
  // ─────────────────────────────────────────────

  Future<void> _showEditDialog(Map<String, dynamic> m) async {
    final nameCtrl = TextEditingController(text: m['mname'] ?? '');
    final emailCtrl = TextEditingController(text: m['email'] ?? '');
    final regionCtrl = TextEditingController(text: m['region'] ?? '');
    String role = (m['role'] ?? 'USER').toString();

    final saved = await showDialog<bool>(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setLocal) => AlertDialog(
          title: Text('회원 수정 - ${m['mid'] ?? ''}'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  controller: nameCtrl,
                  decoration: const InputDecoration(labelText: '이름'),
                ),
                TextField(
                  controller: emailCtrl,
                  decoration: const InputDecoration(labelText: '이메일'),
                ),
                TextField(
                  controller: regionCtrl,
                  decoration: const InputDecoration(labelText: '지역'),
                ),
                const SizedBox(height: 8),
                DropdownButtonFormField<String>(
                  value: role == 'ADMIN' ? 'ADMIN' : 'USER',
                  decoration: const InputDecoration(labelText: '권한'),
                  items: const [
                    DropdownMenuItem(value: 'USER', child: Text('USER')),
                    DropdownMenuItem(value: 'ADMIN', child: Text('ADMIN')),
                  ],
                  onChanged: (v) => setLocal(() => role = v ?? 'USER'),
                ),
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx, false),
              child: const Text('취소'),
            ),
            ElevatedButton(
              onPressed: () => Navigator.pop(ctx, true),
              child: const Text('저장'),
            ),
          ],
        ),
      ),
    );

    if (saved == true) {
      final ok = await _updateMember(m['id'] as int, {
        'mname': nameCtrl.text.trim(),
        'email': emailCtrl.text.trim(),
        'region': regionCtrl.text.trim(),
        'role': role,
      });
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(ok ? '수정 완료' : '수정 실패')),
      );
      if (ok) _fetchMembers();
    }
  }

  Future<void> _confirmDelete(Map<String, dynamic> m) async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('회원 삭제'),
        content: Text('${m['mname'] ?? m['mid'] ?? ''} 회원을 삭제할까요?\n삭제 후 복구는 불가능합니다.'),
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
      final success = await _deleteMember(m['id'] as int);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
            content: Text(success ? '삭제 완료' : '삭제 실패 (연관 데이터 존재 가능)')),
      );
      if (success) _fetchMembers();
    }
  }

  // ─────────────────────────────────────────────
  // UI
  // ─────────────────────────────────────────────

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('회원 관리 (총 ${_members.length}명)'),
        centerTitle: true,
        actions: [
          IconButton(
              icon: const Icon(Icons.refresh), onPressed: _fetchMembers),
        ],
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(12),
            child: TextField(
              decoration: const InputDecoration(
                hintText: '아이디, 이름 또는 이메일 검색',
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
                    ? const Center(child: Text('회원이 없습니다.'))
                    : ListView.builder(
                        itemCount: _filtered.length,
                        itemBuilder: (_, i) {
                          final m =
                              _filtered[i] as Map<String, dynamic>;
                          final isAdmin = m['role'] == 'ADMIN';
                          return ListTile(
                            leading: CircleAvatar(
                              backgroundColor: isAdmin
                                  ? Colors.indigo.withOpacity(0.15)
                                  : Colors.blue.withOpacity(0.12),
                              child: Text(
                                (m['mname'] ?? m['mid'] ?? '?')
                                    .toString()[0]
                                    .toUpperCase(),
                                style: TextStyle(
                                    color: isAdmin
                                        ? Colors.indigo
                                        : Colors.blue,
                                    fontWeight: FontWeight.bold),
                              ),
                            ),
                            title: Row(
                              children: [
                                Text(m['mname'] ?? '-',
                                    style: const TextStyle(
                                        fontWeight: FontWeight.w500)),
                                const SizedBox(width: 8),
                                if (isAdmin)
                                  Container(
                                    padding: const EdgeInsets.symmetric(
                                        horizontal: 6, vertical: 2),
                                    decoration: BoxDecoration(
                                      color: Colors.indigo,
                                      borderRadius:
                                          BorderRadius.circular(4),
                                    ),
                                    child: const Text('ADMIN',
                                        style: TextStyle(
                                            color: Colors.white,
                                            fontSize: 10)),
                                  ),
                              ],
                            ),
                            subtitle: Text(
                                '${m['mid'] ?? '-'} · ${m['email'] ?? '-'}\n지역: ${m['region'] ?? '-'}'),
                            isThreeLine: true,
                            trailing: Row(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                IconButton(
                                  icon: const Icon(Icons.edit,
                                      color: Colors.blue),
                                  tooltip: '수정',
                                  onPressed: () => _showEditDialog(m),
                                ),
                                IconButton(
                                  icon: const Icon(Icons.delete,
                                      color: Colors.red),
                                  tooltip: '삭제',
                                  onPressed: () => _confirmDelete(m),
                                ),
                              ],
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

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../controller/reserve_controller.dart';

/// 도서관 열람실/스터디룸 등 시설 예약 화면
/// - 상단: 예약 신청 폼
/// - 하단: 내 예약 현황 목록
class FacilityReserveScreen extends StatefulWidget {
  const FacilityReserveScreen({super.key});

  @override
  State<FacilityReserveScreen> createState() => _FacilityReserveScreenState();
}

class _FacilityReserveScreenState extends State<FacilityReserveScreen> {
  final _nameController = TextEditingController();
  final _phoneController = TextEditingController();

  String _selectedFacility = '열람실';
  DateTime _selectedDate = DateTime.now().add(const Duration(days: 1));
  int _participants = 1;

  final List<String> _facilityTypes = ['열람실', '스터디룸', '회의실', '세미나실'];

  @override
  void initState() {
    super.initState();
    Future.microtask(() {
      if (context.mounted) {
        context.read<ReserveController>().fetchReservations();
      }
    });
  }

  @override
  void dispose() {
    _nameController.dispose();
    _phoneController.dispose();
    super.dispose();
  }

  Future<void> _pickDate() async {
    final picked = await showDatePicker(
      context: context,
      initialDate: _selectedDate,
      firstDate: DateTime.now(),
      lastDate: DateTime.now().add(const Duration(days: 60)),
      helpText: '예약 날짜 선택',
    );
    if (picked != null) {
      setState(() => _selectedDate = picked);
    }
  }

  Future<void> _submit() async {
    final name = _nameController.text.trim();
    final phone = _phoneController.text.trim();

    if (name.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('신청자 이름을 입력해주세요.')),
      );
      return;
    }

    final reserveDate =
        '${_selectedDate.year}-${_selectedDate.month.toString().padLeft(2, '0')}-${_selectedDate.day.toString().padLeft(2, '0')}';

    final controller = context.read<ReserveController>();
    final success = await controller.createReservation(
      facilityType: _selectedFacility,
      reserveDate: reserveDate,
      applicantName: name,
      phone: phone,
      participants: _participants,
    );

    if (!context.mounted) return;

    if (success) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('예약이 신청되었습니다.')),
      );
      _nameController.clear();
      _phoneController.clear();
      setState(() {
        _participants = 1;
        _selectedDate = DateTime.now().add(const Duration(days: 1));
      });
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('예약 신청에 실패했습니다. 다시 시도해주세요.')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final isLoading = context.watch<ReserveController>().isLoading;
    final reservations = context.watch<ReserveController>().reservationList;

    return Scaffold(
      appBar: AppBar(title: const Text('도서관 시설 예약')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // ── 예약 신청 폼 ──────────────────────────────────
            Card(
              elevation: 2,
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text('예약 신청',
                        style: TextStyle(
                            fontSize: 16, fontWeight: FontWeight.bold)),
                    const SizedBox(height: 16),

                    // 시설 종류 선택
                    DropdownButtonFormField<String>(
                      value: _selectedFacility,
                      decoration: const InputDecoration(
                        labelText: '시설 종류',
                        border: OutlineInputBorder(),
                      ),
                      items: _facilityTypes
                          .map((f) =>
                              DropdownMenuItem(value: f, child: Text(f)))
                          .toList(),
                      onChanged: (v) {
                        if (v != null) setState(() => _selectedFacility = v);
                      },
                    ),
                    const SizedBox(height: 12),

                    // 예약 날짜 선택
                    InkWell(
                      onTap: _pickDate,
                      child: InputDecorator(
                        decoration: const InputDecoration(
                          labelText: '예약 날짜',
                          border: OutlineInputBorder(),
                          suffixIcon: Icon(Icons.calendar_today),
                        ),
                        child: Text(
                          '${_selectedDate.year}년 ${_selectedDate.month}월 ${_selectedDate.day}일',
                        ),
                      ),
                    ),
                    const SizedBox(height: 12),

                    // 신청자 이름
                    TextField(
                      controller: _nameController,
                      decoration: const InputDecoration(
                        labelText: '신청자 이름 *',
                        border: OutlineInputBorder(),
                      ),
                    ),
                    const SizedBox(height: 12),

                    // 연락처
                    TextField(
                      controller: _phoneController,
                      keyboardType: TextInputType.phone,
                      decoration: const InputDecoration(
                        labelText: '연락처 (선택)',
                        hintText: '010-0000-0000',
                        border: OutlineInputBorder(),
                      ),
                    ),
                    const SizedBox(height: 12),

                    // 인원 수 선택
                    Row(
                      children: [
                        const Text('이용 인원:',
                            style: TextStyle(fontSize: 15)),
                        const SizedBox(width: 16),
                        IconButton(
                          icon: const Icon(Icons.remove_circle_outline),
                          onPressed: _participants > 1
                              ? () => setState(() => _participants--)
                              : null,
                        ),
                        Text('$_participants 명',
                            style: const TextStyle(
                                fontSize: 16, fontWeight: FontWeight.bold)),
                        IconButton(
                          icon: const Icon(Icons.add_circle_outline),
                          onPressed: _participants < 10
                              ? () => setState(() => _participants++)
                              : null,
                        ),
                      ],
                    ),
                    const SizedBox(height: 16),

                    // 신청 버튼
                    SizedBox(
                      width: double.infinity,
                      height: 48,
                      child: ElevatedButton.icon(
                        onPressed: isLoading ? null : _submit,
                        icon: isLoading
                            ? const SizedBox(
                                width: 18,
                                height: 18,
                                child: CircularProgressIndicator(
                                    strokeWidth: 2,
                                    color: Colors.white),
                              )
                            : const Icon(Icons.check),
                        label: Text(isLoading ? '신청 중...' : '예약 신청'),
                      ),
                    ),
                  ],
                ),
              ),
            ),

            const SizedBox(height: 24),

            // ── 내 예약 현황 ──────────────────────────────────
            const Text('내 예약 현황',
                style:
                    TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
            const SizedBox(height: 8),

            if (reservations.isEmpty)
              const Center(
                child: Padding(
                  padding: EdgeInsets.symmetric(vertical: 24),
                  child: Text('예약 내역이 없습니다.',
                      style: TextStyle(color: Colors.grey)),
                ),
              )
            else
              ListView.separated(
                physics: const NeverScrollableScrollPhysics(),
                shrinkWrap: true,
                itemCount: reservations.length,
                separatorBuilder: (_, __) => const Divider(height: 1),
                itemBuilder: (context, index) {
                  final item = reservations[index];
                  return ListTile(
                    leading: const Icon(Icons.meeting_room_outlined),
                    title: Text(item.facilityName ?? '미확인 시설'),
                    subtitle: Text(item.applyDate ?? '날짜 미확인'),
                    trailing: Chip(
                      label: Text(
                        item.status ?? '대기중',
                        style: const TextStyle(fontSize: 12),
                      ),
                      backgroundColor: _statusColor(item.status),
                    ),
                  );
                },
              ),
          ],
        ),
      ),
    );
  }

  Color _statusColor(String? status) {
    switch (status) {
      case '승인':
      case 'APPROVED':
        return Colors.green.shade100;
      case '거절':
      case 'REJECTED':
        return Colors.red.shade100;
      default:
        return Colors.orange.shade100;
    }
  }
}

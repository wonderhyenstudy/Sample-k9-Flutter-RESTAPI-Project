import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../controller/rental_controller.dart';
import '../../controller/inquiry_controller.dart';
import '../../controller/reserve_controller.dart';
import '../../widget/common/loading_widget.dart';
import '../../widget/common/empty_widget.dart';
import '../../widget/common/library_card_tile.dart';

/// 내 서비스 탭 — 중첩 TabBar 구조
/// Tab 0: 대여 현황
/// Tab 1: 1:1 문의
/// Tab 2: 시설 예약
class MyServiceTab extends StatefulWidget {
  const MyServiceTab({super.key});

  @override
  State<MyServiceTab> createState() => _MyServiceTabState();
}

class _MyServiceTabState extends State<MyServiceTab>
    with SingleTickerProviderStateMixin {
  late final TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 3, vsync: this);
    Future.microtask(() {
      if (!mounted) return;
      context.read<RentalController>().fetchMemberRentals();
      context.read<InquiryController>().fetchMyInquiries();
      context.read<ReserveController>().fetchReservations();
    });
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        // ── 중첩 탭바 ──────────────────────────────────
        Material(
          color: Theme.of(context).colorScheme.surface,
          elevation: 1,
          child: TabBar(
            controller: _tabController,
            labelColor: Theme.of(context).primaryColor,
            unselectedLabelColor: Colors.grey,
            indicatorColor: Theme.of(context).primaryColor,
            tabs: const [
              Tab(icon: Icon(Icons.import_contacts), text: '대여 현황'),
              Tab(icon: Icon(Icons.question_answer_outlined), text: '1:1 문의'),
              Tab(icon: Icon(Icons.meeting_room_outlined), text: '시설 예약'),
            ],
          ),
        ),
        Expanded(
          child: TabBarView(
            controller: _tabController,
            children: const [
              _RentalContent(),
              _InquiryContent(),
              _ReserveContent(),
            ],
          ),
        ),
      ],
    );
  }
}

// ─────────────────────────────────────────────────────────
// 대여 현황 탭 콘텐츠
// ─────────────────────────────────────────────────────────
class _RentalContent extends StatelessWidget {
  const _RentalContent();

  String _statusLabel(String? s) {
    switch (s) {
      case 'RENTING':
      case 'ACTIVE':
        return '대여중';
      case 'RETURNED':
        return '반납완료';
      case 'OVERDUE':
        return '연체';
      default:
        return s ?? '알 수 없음';
    }
  }

  Color _statusColor(String? s) {
    switch (s) {
      case 'OVERDUE':
        return Colors.red;
      case 'RETURNED':
        return Colors.grey;
      default:
        return Colors.blue;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Consumer<RentalController>(
      builder: (_, ctrl, __) {
        if (ctrl.isLoading) return const LoadingWidget(message: '대여 현황 불러오는 중...');
        if (ctrl.errorMessage != null) {
          return EmptyWidget(
              message: ctrl.errorMessage!, icon: Icons.error_outline);
        }
        if (ctrl.rentalList.isEmpty) {
          return const EmptyWidget(
              message: '대여 내역이 없습니다.', icon: Icons.import_contacts);
        }
        return RefreshIndicator(
          onRefresh: () => ctrl.fetchMemberRentals(),
          child: ListView.builder(
            padding: const EdgeInsets.only(top: 8, bottom: 16),
            itemCount: ctrl.rentalList.length,
            itemBuilder: (_, i) {
              final r = ctrl.rentalList[i];
              return LibraryCardTile(
                leadingIcon: Icons.book_outlined,
                iconColor: _statusColor(r.status),
                title: r.bookTitle ?? '도서 #${r.bookId ?? '-'}',
                subtitle: '대여일: ${r.rentalDate ?? '-'}  반납예정: ${r.dueDate ?? '-'}',
                trailingWidget: Chip(
                  label: Text(_statusLabel(r.status),
                      style: const TextStyle(
                          fontSize: 11, color: Colors.white)),
                  backgroundColor: _statusColor(r.status),
                  padding: EdgeInsets.zero,
                  materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
                ),
              );
            },
          ),
        );
      },
    );
  }
}

// ─────────────────────────────────────────────────────────
// 1:1 문의 탭 콘텐츠
// ─────────────────────────────────────────────────────────
class _InquiryContent extends StatelessWidget {
  const _InquiryContent();

  @override
  Widget build(BuildContext context) {
    return Consumer<InquiryController>(
      builder: (_, ctrl, __) {
        if (ctrl.isLoading) return const LoadingWidget(message: '문의 내역 불러오는 중...');
        if (ctrl.inquiryList.isEmpty) {
          return Stack(
            children: [
              const EmptyWidget(
                  message: '문의 내역이 없습니다.\n궁금한 점을 문의해보세요.',
                  icon: Icons.question_answer_outlined),
              Positioned(
                bottom: 24,
                right: 16,
                child: FloatingActionButton.extended(
                  heroTag: 'inquiry_fab',
                  onPressed: () =>
                      Navigator.pushNamed(context, '/inquiryWrite'),
                  icon: const Icon(Icons.edit),
                  label: const Text('문의 작성'),
                ),
              ),
            ],
          );
        }
        return Stack(
          children: [
            RefreshIndicator(
              onRefresh: () => ctrl.fetchMyInquiries(),
              child: ListView.builder(
                padding: const EdgeInsets.only(top: 8, bottom: 80),
                itemCount: ctrl.inquiryList.length,
                itemBuilder: (_, i) {
                  final q = ctrl.inquiryList[i];
                  final replied = q.isReplied == true;
                  return LibraryCardTile(
                    leadingIcon: Icons.help_outline,
                    iconColor: replied ? Colors.green : Colors.orange,
                    title: q.title ?? '문의',
                    subtitle: q.regDate,
                    trailingWidget: Chip(
                      label: Text(replied ? '답변완료' : '답변대기',
                          style: const TextStyle(
                              fontSize: 11, color: Colors.white)),
                      backgroundColor:
                          replied ? Colors.green : Colors.orange,
                      padding: EdgeInsets.zero,
                      materialTapTargetSize:
                          MaterialTapTargetSize.shrinkWrap,
                    ),
                  );
                },
              ),
            ),
            Positioned(
              bottom: 24,
              right: 16,
              child: FloatingActionButton.extended(
                heroTag: 'inquiry_fab2',
                onPressed: () =>
                    Navigator.pushNamed(context, '/inquiryWrite'),
                icon: const Icon(Icons.edit),
                label: const Text('문의 작성'),
              ),
            ),
          ],
        );
      },
    );
  }
}

// ─────────────────────────────────────────────────────────
// 시설 예약 탭 콘텐츠
// ─────────────────────────────────────────────────────────
class _ReserveContent extends StatefulWidget {
  const _ReserveContent();

  @override
  State<_ReserveContent> createState() => _ReserveContentState();
}

class _ReserveContentState extends State<_ReserveContent> {
  final _nameController = TextEditingController();
  final _phoneController = TextEditingController();
  String _selectedFacility = '열람실';
  DateTime _selectedDate = DateTime.now().add(const Duration(days: 1));
  int _participants = 1;

  static const List<String> _facilityTypes = [
    '열람실', '스터디룸', '회의실', '세미나실'
  ];

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
    if (picked != null && mounted) setState(() => _selectedDate = picked);
  }

  Future<void> _submit() async {
    final name = _nameController.text.trim();
    if (name.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('신청자 이름을 입력해주세요.')));
      return;
    }
    final dateStr =
        '${_selectedDate.year}-${_selectedDate.month.toString().padLeft(2, '0')}-${_selectedDate.day.toString().padLeft(2, '0')}';
    final ctrl = context.read<ReserveController>();
    final ok = await ctrl.createReservation(
      facilityType: _selectedFacility,
      reserveDate: dateStr,
      applicantName: name,
      phone: _phoneController.text.trim(),
      participants: _participants,
    );
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(ok ? '예약이 신청되었습니다.' : '예약 신청 실패. 다시 시도해주세요.')));
    if (ok) {
      _nameController.clear();
      _phoneController.clear();
      setState(() {
        _participants = 1;
        _selectedDate = DateTime.now().add(const Duration(days: 1));
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final ctrl = context.watch<ReserveController>();
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 예약 신청 폼
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('예약 신청',
                      style: TextStyle(
                          fontSize: 15, fontWeight: FontWeight.bold)),
                  const SizedBox(height: 12),
                  DropdownButtonFormField<String>(
                    value: _selectedFacility,
                    decoration: const InputDecoration(
                        labelText: '시설 종류', border: OutlineInputBorder()),
                    items: _facilityTypes
                        .map((f) =>
                            DropdownMenuItem(value: f, child: Text(f)))
                        .toList(),
                    onChanged: (v) {
                      if (v != null) setState(() => _selectedFacility = v);
                    },
                  ),
                  const SizedBox(height: 10),
                  InkWell(
                    onTap: _pickDate,
                    child: InputDecorator(
                      decoration: const InputDecoration(
                        labelText: '예약 날짜',
                        border: OutlineInputBorder(),
                        suffixIcon: Icon(Icons.calendar_today),
                      ),
                      child: Text(
                          '${_selectedDate.year}년 ${_selectedDate.month}월 ${_selectedDate.day}일'),
                    ),
                  ),
                  const SizedBox(height: 10),
                  TextField(
                    controller: _nameController,
                    decoration: const InputDecoration(
                        labelText: '신청자 이름 *',
                        border: OutlineInputBorder()),
                  ),
                  const SizedBox(height: 10),
                  TextField(
                    controller: _phoneController,
                    keyboardType: TextInputType.phone,
                    decoration: const InputDecoration(
                        labelText: '연락처 (선택)',
                        border: OutlineInputBorder()),
                  ),
                  const SizedBox(height: 10),
                  Row(children: [
                    const Text('이용 인원:', style: TextStyle(fontSize: 14)),
                    const SizedBox(width: 12),
                    IconButton(
                      icon: const Icon(Icons.remove_circle_outline),
                      onPressed: _participants > 1
                          ? () => setState(() => _participants--)
                          : null,
                    ),
                    Text('$_participants 명',
                        style: const TextStyle(
                            fontSize: 15, fontWeight: FontWeight.bold)),
                    IconButton(
                      icon: const Icon(Icons.add_circle_outline),
                      onPressed: _participants < 10
                          ? () => setState(() => _participants++)
                          : null,
                    ),
                  ]),
                  const SizedBox(height: 12),
                  SizedBox(
                    width: double.infinity,
                    height: 44,
                    child: ElevatedButton(
                      onPressed: ctrl.isLoading ? null : _submit,
                      child: ctrl.isLoading
                          ? const SizedBox(
                              width: 18,
                              height: 18,
                              child: CircularProgressIndicator(
                                  strokeWidth: 2, color: Colors.white))
                          : const Text('예약 신청'),
                    ),
                  ),
                ],
              ),
            ),
          ),

          const SizedBox(height: 16),
          const Text('내 예약 현황',
              style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),

          if (ctrl.reservationList.isEmpty)
            const EmptyWidget(
                message: '예약 내역이 없습니다.', icon: Icons.meeting_room_outlined)
          else
            ListView.builder(
              physics: const NeverScrollableScrollPhysics(),
              shrinkWrap: true,
              itemCount: ctrl.reservationList.length,
              itemBuilder: (_, i) {
                final r = ctrl.reservationList[i];
                return LibraryCardTile(
                  leadingIcon: Icons.meeting_room_outlined,
                  title: r.facilityName ?? '미확인 시설',
                  subtitle: r.applyDate ?? '날짜 미확인',
                  trailingText: r.status ?? '대기중',
                );
              },
            ),
        ],
      ),
    );
  }
}

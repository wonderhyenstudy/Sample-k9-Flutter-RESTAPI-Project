import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../controller/auth/login_controller.dart';
import '../../widget/common/loading_widget.dart';

/// 마이페이지 탭
/// - 회원 정보 카드 (이름, 이메일, 지역, 가입일)
/// - 바로가기 메뉴 (대여내역, 문의내역, 시설예약)
/// - 로그아웃
class MyPageTab extends StatefulWidget {
  const MyPageTab({super.key});

  @override
  State<MyPageTab> createState() => _MyPageTabState();
}

class _MyPageTabState extends State<MyPageTab> {
  @override
  void initState() {
    super.initState();
    Future.microtask(() {
      if (mounted) context.read<LoginController>().loadMemberInfo();
    });
  }

  @override
  Widget build(BuildContext context) {
    final ctrl = context.watch<LoginController>();

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        // ── 프로필 카드 ───────────────────────────────
        Card(
          elevation: 2,
          shape:
              RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: ctrl.isMemberInfoLoading
                ? const SizedBox(
                    height: 80,
                    child: LoadingWidget(message: '회원 정보 불러오는 중...'))
                : Row(
                    children: [
                      CircleAvatar(
                        radius: 36,
                        backgroundColor:
                            Theme.of(context).primaryColor.withOpacity(0.15),
                        child: Text(
                          _initial(ctrl.memberName ?? ctrl.currentMid),
                          style: TextStyle(
                              fontSize: 28,
                              fontWeight: FontWeight.bold,
                              color: Theme.of(context).primaryColor),
                        ),
                      ),
                      const SizedBox(width: 16),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              ctrl.memberName ?? ctrl.currentMid ?? '회원',
                              style: const TextStyle(
                                  fontSize: 18,
                                  fontWeight: FontWeight.bold),
                            ),
                            if (ctrl.memberEmail != null) ...[
                              const SizedBox(height: 4),
                              Text(ctrl.memberEmail!,
                                  style: TextStyle(
                                      color: Colors.grey[600],
                                      fontSize: 13)),
                            ],
                            if (ctrl.memberRegion != null) ...[
                              const SizedBox(height: 2),
                              Row(children: [
                                Icon(Icons.location_on_outlined,
                                    size: 13, color: Colors.grey[500]),
                                const SizedBox(width: 2),
                                Text(ctrl.memberRegion!,
                                    style: TextStyle(
                                        color: Colors.grey[500],
                                        fontSize: 12)),
                              ]),
                            ],
                            if (ctrl.memberRegDate != null) ...[
                              const SizedBox(height: 2),
                              Text('가입일: ${ctrl.memberRegDate}',
                                  style: TextStyle(
                                      color: Colors.grey[400],
                                      fontSize: 11)),
                            ],
                          ],
                        ),
                      ),
                    ],
                  ),
          ),
        ),

        const SizedBox(height: 20),
        const Text('바로가기',
            style:
                TextStyle(fontSize: 14, color: Colors.grey)),
        const SizedBox(height: 8),

        // ── 바로가기 메뉴 ──────────────────────────────
        _MenuTile(
          icon: Icons.import_contacts,
          color: Colors.blue,
          title: '내 대여 현황',
          subtitle: '현재 대여 중인 도서를 확인합니다',
          onTap: () => Navigator.pushNamed(context, '/rentalList'),
        ),
        _MenuTile(
          icon: Icons.question_answer_outlined,
          color: Colors.orange,
          title: '1:1 문의 내역',
          subtitle: '내가 작성한 문의를 확인합니다',
          onTap: () => Navigator.pushNamed(context, '/inquiryList'),
        ),
        _MenuTile(
          icon: Icons.meeting_room_outlined,
          color: Colors.green,
          title: '시설 예약',
          subtitle: '열람실, 스터디룸 예약 신청',
          onTap: () => Navigator.pushNamed(context, '/facilityReserve'),
        ),

        const SizedBox(height: 16),
        const Divider(),

        // ── 로그아웃 ──────────────────────────────────
        ListTile(
          leading:
              const CircleAvatar(backgroundColor: Colors.red, radius: 20,
                  child: Icon(Icons.logout, color: Colors.white, size: 18)),
          title: const Text('로그아웃',
              style: TextStyle(color: Colors.red, fontWeight: FontWeight.w500)),
          onTap: () => ctrl.showLogoutDialog(context),
        ),
      ],
    );
  }

  String _initial(String? name) {
    if (name == null || name.isEmpty) return '?';
    return name[0].toUpperCase();
  }
}

class _MenuTile extends StatelessWidget {
  final IconData icon;
  final Color color;
  final String title;
  final String subtitle;
  final VoidCallback onTap;

  const _MenuTile({
    required this.icon,
    required this.color,
    required this.title,
    required this.subtitle,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.symmetric(vertical: 4),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: ListTile(
        leading: CircleAvatar(
          backgroundColor: color.withOpacity(0.12),
          child: Icon(icon, color: color),
        ),
        title: Text(title, style: const TextStyle(fontWeight: FontWeight.w500)),
        subtitle: Text(subtitle,
            style: const TextStyle(fontSize: 12, color: Colors.grey)),
        trailing:
            const Icon(Icons.chevron_right, color: Colors.grey),
        onTap: onTap,
      ),
    );
  }
}

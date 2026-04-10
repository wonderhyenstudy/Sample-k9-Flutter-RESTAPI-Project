import 'package:flutter/material.dart';

/// 섹션 헤더 공통 위젯 (타이틀 + 선택적 '더보기' 버튼)
class SectionHeader extends StatelessWidget {
  final String title;
  final String? moreLabel;
  final VoidCallback? onMoreTap;

  const SectionHeader({
    super.key,
    required this.title,
    this.moreLabel = '더보기',
    this.onMoreTap,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 20, 8, 8),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            title,
            style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
          ),
          if (onMoreTap != null)
            TextButton(
              onPressed: onMoreTap,
              child: Text(moreLabel!,
                  style: const TextStyle(fontSize: 13, color: Colors.blueGrey)),
            ),
        ],
      ),
    );
  }
}

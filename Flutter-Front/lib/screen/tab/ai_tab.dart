import 'package:flutter/material.dart';

/// AI 기능 탭
/// - AI 이미지 분석 카드
/// - AI 주가 예측 카드
class AiTab extends StatelessWidget {
  const AiTab({super.key});

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        const SizedBox(height: 8),
        _AiFeatureCard(
          icon: Icons.image_search,
          color: const Color(0xFF1565C0),
          title: 'AI 이미지 분석',
          description: '이미지를 업로드하면 AI가 내용을 분석하고\n결과를 알려드립니다.',
          onTap: () => Navigator.pushNamed(context, '/ai-image'),
        ),
        const SizedBox(height: 16),
        _AiFeatureCard(
          icon: Icons.show_chart,
          color: const Color(0xFF2E7D32),
          title: 'AI 주가 예측',
          description: '삼성전자 주가 데이터를 기반으로\nAI가 미래 주가를 예측합니다.',
          onTap: () => Navigator.pushNamed(context, '/ai-stock'),
        ),
      ],
    );
  }
}

class _AiFeatureCard extends StatelessWidget {
  final IconData icon;
  final Color color;
  final String title;
  final String description;
  final VoidCallback onTap;

  const _AiFeatureCard({
    required this.icon,
    required this.color,
    required this.title,
    required this.description,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 3,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: InkWell(
        borderRadius: BorderRadius.circular(16),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Row(
            children: [
              Container(
                width: 64,
                height: 64,
                decoration: BoxDecoration(
                  color: color.withOpacity(0.12),
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Icon(icon, color: color, size: 32),
              ),
              const SizedBox(width: 20),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(title,
                        style: const TextStyle(
                            fontSize: 17, fontWeight: FontWeight.bold)),
                    const SizedBox(height: 8),
                    Text(description,
                        style: TextStyle(
                            fontSize: 13,
                            color: Colors.grey[600],
                            height: 1.4)),
                  ],
                ),
              ),
              Icon(Icons.arrow_forward_ios, color: Colors.grey[400], size: 16),
            ],
          ),
        ),
      ),
    );
  }
}

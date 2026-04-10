import 'package:flutter/material.dart';

/// 도서관 앱 공통 카드형 ListTile 위젯
class LibraryCardTile extends StatelessWidget {
  final IconData leadingIcon;
  final Color? iconColor;
  final String title;
  final String? subtitle;
  final String? trailingText;
  final Widget? trailingWidget;
  final VoidCallback? onTap;

  const LibraryCardTile({
    super.key,
    this.leadingIcon = Icons.article_outlined,
    this.iconColor,
    required this.title,
    this.subtitle,
    this.trailingText,
    this.trailingWidget,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
      elevation: 1,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
      child: ListTile(
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
        leading: CircleAvatar(
          backgroundColor:
              (iconColor ?? Theme.of(context).primaryColor).withOpacity(0.12),
          child: Icon(leadingIcon,
              color: iconColor ?? Theme.of(context).primaryColor),
        ),
        title: Text(
          title,
          maxLines: 2,
          overflow: TextOverflow.ellipsis,
          style: const TextStyle(fontWeight: FontWeight.w500, fontSize: 14),
        ),
        subtitle: subtitle != null
            ? Text(subtitle!,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style:
                    const TextStyle(fontSize: 12, color: Colors.grey))
            : null,
        trailing: trailingWidget ??
            (trailingText != null
                ? Text(trailingText!,
                    style: const TextStyle(fontSize: 12, color: Colors.grey))
                : const Icon(Icons.chevron_right, color: Colors.grey)),
        onTap: onTap,
      ),
    );
  }
}

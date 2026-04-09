import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../controller/inquiry_controller.dart';
import '../../model/inquiry_model.dart';

/// 1:1 새로운 문의를 작성하여 서버(관리자)에 제출하는 폼 화면
class InquiryWriteScreen extends StatefulWidget {
  const InquiryWriteScreen({super.key});

  @override
  State<InquiryWriteScreen> createState() => _InquiryWriteScreenState();
}

class _InquiryWriteScreenState extends State<InquiryWriteScreen> {
  final TextEditingController _titleController = TextEditingController();
  final TextEditingController _contentController = TextEditingController();

  @override
  void dispose() {
    _titleController.dispose();
    _contentController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    final title = _titleController.text.trim();
    final content = _contentController.text.trim();

    if (title.isEmpty || content.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('제목과 내용을 모두 입력해주세요.')),
      );
      return;
    }

    final inquiry = InquiryModel(title: title, content: content);
    final controller = context.read<InquiryController>();
    final success = await controller.postInquiry(inquiry);

    if (!context.mounted) return;
    if (success) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('문의가 등록되었습니다.')),
      );
      Navigator.pop(context);
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('문의 등록에 실패했습니다. 다시 시도해주세요.')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final isLoading = context.watch<InquiryController>().isLoading;

    return Scaffold(
      appBar: AppBar(title: const Text('문의 작성하기')),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          children: [
            TextField(
              controller: _titleController,
              decoration: const InputDecoration(
                labelText: '제목',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 16),
            Expanded(
              child: TextField(
                controller: _contentController,
                maxLines: null,
                expands: true,
                textAlignVertical: TextAlignVertical.top,
                decoration: const InputDecoration(
                  labelText: '내용',
                  border: OutlineInputBorder(),
                ),
              ),
            ),
            const SizedBox(height: 16),
            SizedBox(
              width: double.infinity,
              height: 50,
              child: ElevatedButton(
                onPressed: isLoading ? null : _submit,
                child: isLoading
                    ? const CircularProgressIndicator()
                    : const Text('작성 완료'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

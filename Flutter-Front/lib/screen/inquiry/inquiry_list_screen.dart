import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../controller/inquiry_controller.dart';

/// 작성 완료된 1:1 문의사항 리스트와 답변 달림 여부를 조회하는 화면
class InquiryListScreen extends StatefulWidget {
  const InquiryListScreen({super.key});

  @override
  State<InquiryListScreen> createState() => _InquiryListScreenState();
}

class _InquiryListScreenState extends State<InquiryListScreen> {
  @override
  void initState() {
    super.initState();
    Future.microtask(() {
      Provider.of<InquiryController>(context, listen: false).fetchMyInquiries();
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('나의 1:1 문의'),
      ),
      body: Consumer<InquiryController>(
        builder: (context, controller, child) {
          if (controller.inquiryList.isEmpty) {
            return const Center(child: Text('작성한 문의 내역이 없습니다.'));
          }

          return ListView.builder(
            itemCount: controller.inquiryList.length,
            itemBuilder: (context, index) {
              final inquiry = controller.inquiryList[index];
              return ListTile(
                leading: const Icon(Icons.help_outline),
                title: Text(inquiry.title ?? '제목 없음'),
                trailing: Text(inquiry.isReplied == true ? '답변완료' : '대기중'),
              );
            },
          );
        },
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {
          Navigator.pushNamed(context, '/inquiryWrite');
        },
        child: const Icon(Icons.add),
      ),
    );
  }
}

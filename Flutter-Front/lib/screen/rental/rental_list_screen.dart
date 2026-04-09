import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../controller/rental_controller.dart';

/// 회원의 도서 대여 이력 및 현재 대여 중인 현황을 보는 화면
class RentalListScreen extends StatefulWidget {
  const RentalListScreen({super.key});

  @override
  State<RentalListScreen> createState() => _RentalListScreenState();
}

class _RentalListScreenState extends State<RentalListScreen> {
  @override
  void initState() {
    super.initState();
    Future.microtask(() {
      Provider.of<RentalController>(context, listen: false).fetchMemberRentals();
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('대여 현황 및 이력'),
      ),
      body: Consumer<RentalController>(
        builder: (context, controller, child) {
          if (controller.isLoading) {
            return const Center(child: CircularProgressIndicator());
          }
          if (controller.rentalList.isEmpty) {
            return const Center(child: Text('대여한 도서 내역이 없습니다.'));
          }

          return ListView.builder(
            itemCount: controller.rentalList.length,
            itemBuilder: (context, index) {
              final rental = controller.rentalList[index];
              return ListTile(
                leading: const Icon(Icons.menu_book),
                title: Text('도서번호: ${rental.bookId}'),
                subtitle: Text('대여: ${rental.rentDate} / 반납: ${rental.returnDate}'),
                trailing: Chip(label: Text(rental.status ?? '알수없음')),
              );
            },
          );
        },
      ),
    );
  }
}

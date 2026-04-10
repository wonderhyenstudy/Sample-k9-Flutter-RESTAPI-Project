class RentalModel {
  final int? id;
  final int? memberId;
  final String? memberName;
  final int? bookId;
  final String? bookTitle;
  final String? bookAuthor;
  final String? rentalDate;  // 대여일 (yyyy-MM-dd)
  final String? dueDate;     // 반납 기한 (yyyy-MM-dd)
  final String? returnDate;  // 실제 반납일 (nullable)
  final String? status;      // RENTING, RETURNED, OVERDUE, EXTENDED
  final bool overdue;

  RentalModel({
    this.id,
    this.memberId,
    this.memberName,
    this.bookId,
    this.bookTitle,
    this.bookAuthor,
    this.rentalDate,
    this.dueDate,
    this.returnDate,
    this.status,
    this.overdue = false,
  });

  factory RentalModel.fromJson(Map<String, dynamic> json) {
    return RentalModel(
      id: json['id'],
      memberId: json['memberId'],
      memberName: json['memberName'],
      bookId: json['bookId'],
      bookTitle: json['bookTitle'],
      bookAuthor: json['bookAuthor'],
      rentalDate: json['rentalDate'],
      dueDate: json['dueDate'],
      returnDate: json['returnDate'],
      status: json['status']?.toString(),
      overdue: json['overdue'] == true,
    );
  }
}

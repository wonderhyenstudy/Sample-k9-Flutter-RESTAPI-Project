class ApplyModel {
  final int? id;
  final int? memberId;
  final String? facilityName;
  final String? applyDate;
  final String? status;

  ApplyModel({this.id, this.memberId, this.facilityName, this.applyDate, this.status});

  factory ApplyModel.fromJson(Map<String, dynamic> json) {
    return ApplyModel(
      id: json['id'],
      memberId: json['memberId'],
      facilityName: json['facilityType'] ?? json['facilityName'], // Spring: facilityType
      applyDate: json['reserveDate'] ?? json['applyDate'],        // Spring: reserveDate
      status: json['status'],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'memberId': memberId,
      'facilityName': facilityName,
      'applyDate': applyDate,
      'status': status,
    };
  }
}

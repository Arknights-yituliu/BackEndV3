CREATE TABLE `operator_progression_manual_data` (
  `ak_uid` varchar(255) NOT NULL,
  `operator_progression` longtext,
  `create_time` datetime(6) DEFAULT NULL,
  `update_time` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`ak_uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

DROP TABLE IF EXISTS `serve_object`;
DROP TABLE IF EXISTS `schedule_log`;
DROP TABLE IF EXISTS `air_condition_log`;
DROP TABLE IF EXISTS `customer`;
create table `serve_object`(
    `id` integer NOT NULL AUTO_INCREMENT,
    `serve_id` integer,
    `ac_id` integer,
    `state` int2,
    `mode` int2,
    `speed` int2,
    `start_tempt` numeric(4,1),
    `target_tempt` numeric(4,1),
    `current_tempt` numeric(4,1),
    `fee_rate` numeric(5,1),
    `fee` numeric(5,1),
    `start_time` timestamp,
    `end_time` timestamp,
    `serve_time` integer,
    primary key (`id`)
);

create table `schedule_log` (
    `id` integer not null,
    `ac_id` integer,
    `type` integer,
    `serve_id` integer,
    `date` timestamp,
    primary key (`id`)
);

create table `air_condition_log`(
    `id` integer not null,
    `ac_id` integer,
    `state` integer,
    `date` timestamp,
    primary key (`id`)
);

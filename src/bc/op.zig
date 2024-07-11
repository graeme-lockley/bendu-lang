pub const Op = enum(u8) {
    ret,
    discard,
    duplicate,

    push_false,
    push_float,
    push_int,
    push_string,
    push_true,
    push_unit,

    push_global,

    call_local,
    ret_local,

    jmp,
    jmp_false,
    jmp_tos_false,
    jmp_tos_true,

    add,
    add_char,
    add_float,
    add_int,
    add_string,
    divide,
    divide_char,
    divide_float,
    divide_int,
    equals,
    equals_bool,
    equals_char,
    equals_float,
    equals_int,
    equals_string,
    greaterequals,
    greaterequals_bool,
    greaterequals_char,
    greaterequals_float,
    greaterequals_int,
    greaterequals_string,
    greaterthan,
    greaterthan_bool,
    greaterthan_char,
    greaterthan_float,
    greaterthan_int,
    greaterthan_string,
    lessequals,
    lessequals_bool,
    lessequals_char,
    lessequals_float,
    lessequals_int,
    lessequals_string,
    lessthan,
    lessthan_bool,
    lessthan_char,
    lessthan_float,
    lessthan_int,
    lessthan_string,
    minus,
    minus_char,
    minus_float,
    minus_int,
    modulo_int,
    notequals,
    notequals_bool,
    notequals_char,
    notequals_float,
    notequals_int,
    notequals_string,
    power,
    power_char,
    power_float,
    power_int,
    times,
    times_char,
    times_float,
    times_int,
    not,

    print_int,
    print_ln,
};

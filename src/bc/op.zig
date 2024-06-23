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

    add,
    add_char,
    add_int,
    add_float,
    add_string,
    not,

    print_int,
    print_ln,
};

const std = @import("std");

const Memory = @import("memory.zig");
const Op = @import("op.zig").Op;
const Pointer = @import("pointer.zig");
const Runtime = @import("runtime.zig");

const DEBUG = false;

pub fn runBytecode(bc: []const u8, runtime: *Runtime.Runtime) !void {
    const package = try runtime.packages.useBytecode(bc, runtime);

    try run(package, runtime);
}

const InterpreterErrors = error{ OutOfMemory, DiskQuota, FileTooBig, InputOutput, NoSpaceLeft, DeviceBusy, InvalidArgument, AccessDenied, BrokenPipe, SystemResources, OperationAborted, NotOpenForWriting, LockViolation, WouldBlock, ConnectionResetByPeer, Unexpected, IsDir, ConnectionTimedOut, NotOpenForReading, SocketNotConnected, InvalidUtf8, ProcessFdQuotaExceeded, SystemFdQuotaExceeded, SharingViolation, PathAlreadyExists, FileNotFound, PipeBusy, NameTooLong, InvalidWtf8, BadPathName, NetworkNotFound, AntivirusInterference, SymLinkLoop, NoDevice, NotDir, FileLocksNotSupported, FileBusy, Unseekable };

pub fn run(initPackage: *Runtime.Package, runtime: *Runtime.Runtime) InterpreterErrors!void {
    if (DEBUG) {
        std.debug.print("run: start: packageID={d}\n", .{initPackage.id});
    }

    var package = initPackage;
    var bc = try package.getByteCode(runtime);
    var ip: usize = 0;
    var fp: usize = runtime.stack.items.len;

    try runtime.push(Pointer.fromPointer(*Memory.Value, try package.getFrame(runtime)));

    while (ip < bc.len) {
        const op = @as(Op, @enumFromInt(bc[ip]));

        ip += 1;
        switch (op) {
            .abort => {
                const code = readi32(bc, ip);

                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: abort, code={d}\n", .{ ip - 1, fp, runtime.stack.items.len, code });
                }

                std.posix.exit(@intCast(code));
            },

            .push_array => {
                const len = readi32(bc, ip);

                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: push_array: len={d}\n", .{ ip - 1, fp, runtime.stack.items.len, len });
                }

                try runtime.push_array(@intCast(len));
                ip += 4;
            },
            .push_array_element => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: push_array_element\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.push_array_element();
            },
            .push_array_range_from => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: push_array_range_from\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.push_array_range_from();
            },
            .push_array_range_to => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: push_array_range_to\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.push_array_range_to();
            },
            .push_array_range => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: push_array_range\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.push_array_range();
            },
            .push_bool_true => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: push_bool_true\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.push_bool_true();
            },

            .push_bool_false => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: push_bool_false\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.push_bool_false();
            },
            .push_closure => {
                const offset: usize = @intCast(readi32(bc, ip));
                const frame: usize = @intCast(readi32(bc, ip + 4));

                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: create_closure: offset={d}, frame={d}\n", .{ ip - 1, fp, runtime.stack.items.len, offset, frame });
                }

                const previousFrame = Memory.FrameValue.skip(Pointer.as(*Memory.Value, runtime.stack.items[fp]), frame);

                try runtime.push_closure(package.id, @intCast(offset), previousFrame.?);

                ip += 8;
            },
            .push_f32_literal => {
                const value = readf32(bc, ip);

                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: push_f32_literal: value={d}\n", .{ ip - 1, fp, runtime.stack.items.len, value });
                }

                try runtime.push_f32_literal(value);
                ip += 4;
            },
            .push_i32_literal => {
                const value = readi32(bc, ip);

                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: push_i32_literal: value={d}\n", .{ ip - 1, fp, runtime.stack.items.len, value });
                }

                try runtime.push_i32_literal(value);
                ip += 4;
            },
            .push_u8_literal => {
                const value = bc[ip];

                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: push_u8_literal: value={d}\n", .{ ip - 1, fp, runtime.stack.items.len, value });
                }

                try runtime.push_u8_literal(value);
                ip += 1;
            },
            .push_package_closure => {
                const packageID = readi32(bc, ip);
                const offset = readi32(bc, ip + 4);

                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: push_package_closure: packageID={d}, offset={d}\n", .{ ip - 1, fp, runtime.stack.items.len, packageID, offset });
                }

                var newPackageID: usize = 0;

                if (packageID < 0) {
                    const newPackage = try Runtime.resolvePackageID(runtime, package, @intCast(-packageID - 1));
                    writei32(bc, ip, @intCast(newPackage.id));
                    newPackageID = newPackage.id;
                } else {
                    newPackageID = @intCast(packageID);
                }

                const newPackage = &runtime.packages.items.items[newPackageID];
                const newPackageFrame = try newPackage.getFrame(runtime);

                try runtime.push_closure(newPackageID, @intCast(offset), newPackageFrame);

                ip += 8;
            },
            .push_string_literal => {
                const len = readi32(bc, ip);
                const data = bc[ip + 4 .. ip + 4 + @as(usize, @intCast(len))];

                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: push_string_literal: len={d}, data={s}\n", .{ ip - 1, fp, runtime.stack.items.len, len, data });
                }

                try runtime.push_string_literal(data);
                ip += 4 + @as(usize, @intCast(len));
            },
            .push_tuple => {
                const arity = readi32(bc, ip);

                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: push_tuple: arity={d}\n", .{ ip - 1, fp, runtime.stack.items.len, arity });
                }

                try runtime.push_tuple(@intCast(arity));
                ip += 4;
            },
            .push_tuple_component => {
                const index = readi32(bc, ip);

                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: push_tuple_component: index={d}\n", .{ ip - 1, fp, runtime.stack.items.len, index });
                }

                try runtime.push_tuple_component(@intCast(index));
                ip += 4;
            },
            .push_unit_literal => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: push_unit_literal\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.push_unit_literal();
            },
            .load => {
                const frame = readi32(bc, ip);
                const offset = readi32(bc, ip + 4);

                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: load: frame={d}, offset={d}\n", .{ ip - 1, fp, runtime.stack.items.len, frame, offset });
                }

                try runtime.load(fp, @intCast(frame), @intCast(offset));
                ip += 8;
            },
            .load_package => {
                const packageID = readi32(bc, ip);
                const offset = readi32(bc, ip + 4);

                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: load_package: packageID={d}, offset={d}\n", .{ ip - 1, fp, runtime.stack.items.len, packageID, offset });
                }

                if (packageID < 0) {
                    const newPackage = try Runtime.resolvePackageID(runtime, package, @intCast(-packageID - 1));
                    // std.debug.print("** load_package: packageID={d}, newPackageID={d}, src={s}\n", .{ packageID, newPackage.id, newPackage.src.slice() });
                    writei32(bc, ip, @intCast(newPackage.id));
                    try runtime.load_package(@intCast(newPackage.id), @intCast(offset));
                } else {
                    try runtime.load_package(@intCast(packageID), @intCast(offset));
                }
                ip += 8;
            },
            .store => {
                const frame = readi32(bc, ip);
                const offset = readi32(bc, ip + 4);

                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: store: frame={d}, offset={d}\n", .{ ip - 1, fp, runtime.stack.items.len, frame, offset });
                }

                try runtime.store(fp, @intCast(frame), @intCast(offset));
                ip += 8;
            },
            .store_package => {
                const packageID = readi32(bc, ip);
                const offset = readi32(bc, ip + 4);

                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: store _package: packageID={d}, offset={d}\n", .{ ip - 1, fp, runtime.stack.items.len, packageID, offset });
                }

                if (packageID < 0) {
                    const newPackage = try Runtime.resolvePackageID(runtime, package, @intCast(-packageID - 1));
                    writei32(bc, ip, @intCast(newPackage.id));
                    try runtime.store_package(@intCast(newPackage.id), @intCast(offset));
                } else {
                    try runtime.store_package(@intCast(packageID), @intCast(offset));
                }
                ip += 8;
            },
            .store_array_element => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: store_array_element\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.store_array_element();
            },
            .store_array_range => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: store_array_range\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.store_array_range();
            },
            .store_array_range_from => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: store_array_range_from\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.store_array_range_from();
            },
            .store_array_range_to => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: store_array_range_to\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.store_array_range_to();
            },
            .array_append_element_duplicate => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: array_append_element_duplicate\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.array_append_element_duplicate();
            },
            .array_prepend_element_duplicate => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: prepend_element_duplicate\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.array_prepend_element_duplicate();
            },
            .array_append_element => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: array_append_element\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.array_append_element();
            },
            .array_append_array => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: array_append_array\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.array_append_array();
            },
            .array_prepend_element => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: prepend_element\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.array_prepend_element();
            },
            .dup => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: dup\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.duplicate();
            },
            .discard => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: discard\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                runtime.discard();
            },

            .jmp => {
                const offset = readi32(bc, ip);

                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: jmp: offset={d}\n", .{ ip - 1, fp, runtime.stack.items.len, offset });
                }

                ip = @intCast(offset);
            },
            .jmp_false => {
                const offset = readi32(bc, ip);

                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: jmp_false: offset={d}\n", .{ ip - 1, fp, runtime.stack.items.len, offset });
                }

                const value = runtime.pop();
                if (Pointer.asBool(value)) {
                    ip += 4;
                } else {
                    ip = @intCast(offset);
                }
            },
            .jmp_dup_false => {
                const offset = readi32(bc, ip);

                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: jmp_dup_false: offset={d}\n", .{ ip - 1, fp, runtime.stack.items.len, offset });
                }

                const value = runtime.peek();
                if (Pointer.asBool(value)) {
                    ip += 4;
                } else {
                    ip = @intCast(offset);
                }
            },
            .jmp_dup_true => {
                const offset = readi32(bc, ip);

                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: jmp_dup_true: offset={d}\n", .{ ip - 1, fp, runtime.stack.items.len, offset });
                }

                const value = runtime.peek();
                if (Pointer.asBool(value)) {
                    ip = @intCast(offset);
                } else {
                    ip += 4;
                }
            },

            .call => {
                const offset: usize = @intCast(readi32(bc, ip));
                const arity: usize = @intCast(readi32(bc, ip + 4));
                const frame: usize = @intCast(readi32(bc, ip + 8));

                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: call: offset={d}, arity={d}, frame={d}\n", .{ ip - 1, fp, runtime.stack.items.len, offset, arity, frame });
                }

                const previousFrame = Pointer.as(*Memory.Value, runtime.stack.items[fp]);

                _ = try runtime.push_frame(Memory.FrameValue.skip(previousFrame, frame));

                const newFramePointer = runtime.pop();

                const newFrame: *Memory.Value = Pointer.as(*Memory.Value, newFramePointer);
                for (0..arity) |i| {
                    try Memory.FrameValue.set(newFrame, 0, arity - i - 1, runtime.pop());
                }
                const oldFP = fp;
                fp = runtime.stack.items.len;
                try runtime.push(newFramePointer);
                try runtime.push(package.id);
                try runtime.push(ip + 12);
                try runtime.push(oldFP);

                ip = offset;
            },
            .call_closure => {
                const arity: usize = @intCast(readi32(bc, ip));

                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: call_closure: arity={d}\n", .{ ip - 1, fp, runtime.stack.items.len, arity });
                }

                const closure = Pointer.as(*Memory.Value, runtime.peekN(arity));

                _ = try runtime.push_frame(closure.v.ClosureKind.frame);

                const newFramePointer = runtime.pop();
                const newFrame: *Memory.Value = Pointer.as(*Memory.Value, newFramePointer);
                for (0..arity) |i| {
                    try Memory.FrameValue.set(newFrame, 0, arity - i - 1, runtime.pop());
                }
                runtime.discard(); // discard the closure
                const oldFP = fp;
                fp = runtime.stack.items.len;
                try runtime.push(newFramePointer);
                try runtime.push(package.id);
                try runtime.push(ip + 4);
                try runtime.push(oldFP);

                ip = closure.v.ClosureKind.function;

                if (package.id != closure.v.ClosureKind.packageID) {
                    package = &runtime.packages.items.items[closure.v.ClosureKind.packageID];
                    bc = try package.getByteCode(runtime);
                }
            },
            .call_package => {
                const packageID = readi32(bc, ip);
                const offset: usize = @intCast(readi32(bc, ip + 4));
                const arity: usize = @intCast(readi32(bc, ip + 8));

                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: call_package: packageID={d}, offset={d}, arity={d}\n", .{ ip - 1, fp, runtime.stack.items.len, packageID, offset, arity });
                }

                var newPackageID: usize = 0;

                if (packageID < 0) {
                    const newPackage = try Runtime.resolvePackageID(runtime, package, @intCast(-packageID - 1));
                    writei32(bc, ip, @intCast(newPackage.id));
                    newPackageID = newPackage.id;
                } else {
                    newPackageID = @intCast(packageID);
                }

                const newPackage = &runtime.packages.items.items[newPackageID];
                const newPackageFrame = try newPackage.getFrame(runtime);

                _ = try runtime.push_frame(newPackageFrame);

                const newFramePointer = runtime.pop();

                const newFrame: *Memory.Value = Pointer.as(*Memory.Value, newFramePointer);
                for (0..arity) |i| {
                    try Memory.FrameValue.set(newFrame, 0, arity - i - 1, runtime.pop());
                }
                const oldFP = fp;
                fp = runtime.stack.items.len;
                try runtime.push(newFramePointer);
                try runtime.push(package.id);
                try runtime.push(ip + 12);
                try runtime.push(oldFP);

                ip = offset;

                if (package.id != newPackageID) {
                    package = &runtime.packages.items.items[newPackageID];
                    bc = try package.getByteCode(runtime);
                }
            },

            .ret => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: ret\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                const v = runtime.pop();
                fp = runtime.pop();
                ip = runtime.pop();
                const newPackageID = runtime.pop();
                _ = runtime.discard();
                try runtime.push(v);

                if (package.id != newPackageID) {
                    package = &runtime.packages.items.items[newPackageID];
                    bc = try package.getByteCode(runtime);
                }
            },

            .not_bool => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: not_bool\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.not_bool();
            },

            .add_f32 => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: add_f32\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.add_f32();
            },
            .add_i32 => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: add_i32\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.add_i32();
            },
            .add_string => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: add_string\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.add_string();
            },
            .add_u8 => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: add_u8\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.add_u8();
            },
            .sub_f32 => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: sub_f32\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.sub_f32();
            },
            .sub_i32 => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: sub_i32\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.sub_i32();
            },
            .sub_u8 => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: sub_u8\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.sub_u8();
            },
            .mul_f32 => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: mul_f32\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.mul_f32();
            },
            .mul_i32 => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: mul_i32\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.mul_i32();
            },
            .mul_u8 => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: mul_u8\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.mul_u8();
            },
            .div_f32 => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: div_f32\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.div_f32();
            },
            .div_i32 => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: div_i32\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.div_i32();
            },
            .div_u8 => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: div_u8\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.div_u8();
            },
            .mod_i32 => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: mod_i32\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.mod_i32();
            },
            .pow_f32 => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: pow_f32\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.pow_f32();
            },
            .pow_i32 => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: pow_i32\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.pow_i32();
            },

            .eq => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: eq\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.eq();
            },
            .eq_bool => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: eq_bool\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.eq_bool();
            },
            .eq_f32 => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: eq_f32\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.eq_f32();
            },
            .eq_i32 => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: eq_i32\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.eq_i32();
            },
            .eq_string => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: eq_string\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.eq_string();
            },
            .eq_u8 => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: eq_u8\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.eq_u8();
            },
            .eq_unit => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: eq_unit\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.eq_unit();
            },
            .neq => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: neq\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.neq();
            },
            .neq_bool => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: neq_bool\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.neq_bool();
            },
            .neq_f32 => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: neq_f32\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.neq_f32();
            },
            .neq_i32 => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: neq_i32\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.neq_i32();
            },
            .neq_string => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: neq_string\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.neq_string();
            },
            .neq_u8 => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: neq_u8\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.neq_u8();
            },
            .neq_unit => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: neq_unit\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.neq_unit();
            },
            .lt_f32 => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: lt_f32\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.lt_f32();
            },
            .lt_i32 => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: lt_i32\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.lt_i32();
            },
            .lt_string => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: lt_string\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.lt_string();
            },
            .lt_u8 => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: lt_u8\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.lt_u8();
            },
            .le_f32 => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: le_f32\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.le_f32();
            },
            .le_i32 => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: le_i32\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.le_i32();
            },
            .le_string => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: le_string\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.le_string();
            },
            .le_u8 => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: le_u8\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.le_u8();
            },
            .gt_f32 => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: gt_f32\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.gt_f32();
            },
            .gt_i32 => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: gt_i32\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.gt_i32();
            },
            .gt_string => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: gt_string\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.gt_string();
            },
            .gt_u8 => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: gt_u8\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.gt_u8();
            },
            .ge_f32 => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: ge_f32\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.ge_f32();
            },
            .ge_i32 => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: ge_i32\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.ge_i32();
            },
            .ge_string => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: ge_string\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.ge_string();
            },
            .ge_u8 => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: ge_u8\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.ge_u8();
            },

            .print => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: print\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.print();
            },
            .println => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: println\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.println();
            },
            .print_bool => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: print_bool\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.print_bool();
            },
            .print_f32 => {
                if (DEBUG) {
                    const value = runtime.peek();
                    std.debug.print("{d} {d} {d}: print_f32: value={d}\n", .{ ip - 1, fp, runtime.stack.items.len, Pointer.asInt(value) });
                }

                try runtime.print_f32();
            },
            .print_i32 => {
                if (DEBUG) {
                    const value = runtime.peek();
                    std.debug.print("{d} {d} {d}: print_i32: value={d}\n", .{ ip - 1, fp, runtime.stack.items.len, Pointer.asInt(value) });
                }

                try runtime.print_i32();
            },
            .print_u8 => {
                if (DEBUG) {
                    const value = runtime.peek();
                    std.debug.print("{d} {d} {d}: print_u8: value={c}\n", .{ ip - 1, fp, runtime.stack.items.len, Pointer.asChar(value) });
                }

                try runtime.print_u8();
            },
            .print_string => {
                if (DEBUG) {
                    const value = runtime.peek();
                    std.debug.print("{d} {d} {d}: print_string: value={s}\n", .{ ip - 1, fp, runtime.stack.items.len, Pointer.asString(value).data });
                }

                try runtime.print_string();
            },
            .print_unit => {
                if (DEBUG) {
                    std.debug.print("{d} {d} {d}: print_unit\n", .{ ip - 1, fp, runtime.stack.items.len });
                }

                try runtime.print_unit();
            },

            else => std.debug.panic("unknown op code: {d}\n", .{bc[ip - 1]}),
        }
    }
    if (DEBUG) {
        std.debug.print("run: exit: packageID={d}\n", .{package.id});
    }
}

fn readi32(bc: []const u8, ip: usize) i32 {
    const v: i32 = @bitCast(@as(u32, (bc[ip + 3])) |
        (@as(u32, bc[ip + 2]) << 8) |
        (@as(u32, bc[ip + 1]) << 16) |
        (@as(u32, bc[ip]) << 24));

    return v;
}

fn writei32(bc: []u8, ip: usize, value: i32) void {
    bc[ip] = @intCast(value >> 24);
    bc[ip + 1] = @intCast(value >> 16);
    bc[ip + 2] = @intCast(value >> 8);
    bc[ip + 3] = @intCast(value);
}

fn readf32(bc: []const u8, ip: usize) f32 {
    const v: f32 = @bitCast(@as(u32, (bc[ip + 3])) |
        (@as(u32, bc[ip + 2]) << 8) |
        (@as(u32, bc[ip + 1]) << 16) |
        (@as(u32, bc[ip]) << 24));

    return v;
}

const maxPackageID = std.math.maxInt(usize);

test "push_bool_true" {
    const bc: [1]u8 = [_]u8{@intFromEnum(Op.push_bool_true)};
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();
    try runBytecode(&bc, &runtime);

    try std.testing.expectEqual(runtime.stack.items.len, 2);
    try std.testing.expectEqual(Pointer.asBool(runtime.pop()), true);
}

test "push_bool_false" {
    const bc: [1]u8 = [_]u8{@intFromEnum(Op.push_bool_false)};
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();
    try runBytecode(&bc, &runtime);

    try std.testing.expectEqual(runtime.stack.items.len, 2);
    try std.testing.expectEqual(Pointer.asBool(runtime.pop()), false);
}

test "push_f32_literal" {
    const bc: [5]u8 = [_]u8{ @intFromEnum(Op.push_f32_literal), 0, 1, 0, 0 };
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();
    try runBytecode(&bc, &runtime);

    try std.testing.expectEqual(runtime.stack.items.len, 2);
    try std.testing.expectEqual(Pointer.asFloat(runtime.pop()), 9.1835e-41);
}

test "push_i32_literal" {
    const bc: [5]u8 = [_]u8{ @intFromEnum(Op.push_i32_literal), 0, 0, 0, 42 };
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();
    try runBytecode(&bc, &runtime);

    try std.testing.expectEqual(runtime.stack.items.len, 2);
    try std.testing.expectEqual(Pointer.asInt(runtime.pop()), 42);
}

test "push_u8_literal" {
    const bc: [2]u8 = [_]u8{ @intFromEnum(Op.push_u8_literal), 42 };
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();
    try runBytecode(&bc, &runtime);

    try std.testing.expectEqual(runtime.stack.items.len, 2);
    try std.testing.expectEqual(Pointer.asChar(runtime.pop()), 42);
}

test "store and load - int" {
    const bc: [23]u8 = [_]u8{ @intFromEnum(Op.push_i32_literal), 0, 0, 0, 42, @intFromEnum(Op.store), 0, 0, 0, 0, 0, 0, 0, 10, @intFromEnum(Op.load), 0, 0, 0, 0, 0, 0, 0, 10 };
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();
    try runBytecode(&bc, &runtime);

    try std.testing.expectEqual(runtime.stack.items.len, 2);
    try std.testing.expectEqual(Pointer.asInt(runtime.pop()), 42);
}

test "store and load - int 2" {
    const bc: [23]u8 = [_]u8{ @intFromEnum(Op.push_i32_literal), 0, 0, 0, 42, @intFromEnum(Op.store), 0, 0, 0, 0, 0, 0, 0, 0, @intFromEnum(Op.load), 0, 0, 0, 0, 0, 0, 0, 0 };
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();
    try runBytecode(&bc, &runtime);

    try std.testing.expectEqual(runtime.stack.items.len, 2);
    try std.testing.expectEqual(Pointer.asInt(runtime.pop()), 42);
}

test "store and load - string" {
    const bc: [34]u8 = [_]u8{ @intFromEnum(Op.push_string_literal), 0, 0, 0, 11, 'h', 'e', 'l', 'l', 'o', ' ', 'w', 'o', 'r', 'l', 'd', @intFromEnum(Op.store), 0, 0, 0, 0, 0, 0, 0, 10, @intFromEnum(Op.load), 0, 0, 0, 0, 0, 0, 0, 10 };
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();
    try runBytecode(&bc, &runtime);

    try std.testing.expectEqual(runtime.stack.items.len, 2);
    try std.testing.expect(std.mem.eql(u8, Pointer.asString(runtime.peek()).slice(), "hello world"));
}

test "not_bool" {
    const bc: [2]u8 = [_]u8{ @intFromEnum(Op.push_bool_true), @intFromEnum(Op.not_bool) };
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();
    try runBytecode(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asBool(runtime.pop()), false);
}

test "add_f32" {
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [11]u8 = [_]u8{ @intFromEnum(Op.push_f32_literal), 66, 200, 0, 0, @intFromEnum(Op.push_f32_literal), 66, 40, 0, 0, @intFromEnum(Op.add_f32) };
    //                                                           100.0                                             42.0
    try runBytecode(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asFloat(runtime.pop()), 142.0);
}
test "add_i32" {
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [11]u8 = [_]u8{ @intFromEnum(Op.push_i32_literal), 0, 0, 0, 100, @intFromEnum(Op.push_i32_literal), 0, 0, 0, 42, @intFromEnum(Op.add_i32) };
    try runBytecode(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asInt(runtime.pop()), 142);
}
test "add_string" {
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [22]u8 = [_]u8{ @intFromEnum(Op.push_string_literal), 0, 0, 0, 5, 'h', 'e', 'l', 'l', 'o', @intFromEnum(Op.push_string_literal), 0, 0, 0, 6, ' ', 'w', 'o', 'r', 'l', 'd', @intFromEnum(Op.add_string) };
    try runBytecode(&bc, &runtime);

    try std.testing.expectEqual(runtime.stack.items.len, 2);
    try std.testing.expect(std.mem.eql(u8, Pointer.asString(runtime.peek()).slice(), "hello world"));
}
test "add_u8" {
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [5]u8 = [_]u8{ @intFromEnum(Op.push_u8_literal), 100, @intFromEnum(Op.push_u8_literal), 42, @intFromEnum(Op.add_u8) };
    try runBytecode(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asChar(runtime.pop()), 142);
}

test "sub_f32" {
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [11]u8 = [_]u8{ @intFromEnum(Op.push_f32_literal), 66, 200, 0, 0, @intFromEnum(Op.push_f32_literal), 66, 40, 0, 0, @intFromEnum(Op.sub_f32) };
    try runBytecode(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asFloat(runtime.pop()), 58.0);
}
test "sub_i32" {
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [11]u8 = [_]u8{ @intFromEnum(Op.push_i32_literal), 0, 0, 0, 100, @intFromEnum(Op.push_i32_literal), 0, 0, 0, 42, @intFromEnum(Op.sub_i32) };
    try runBytecode(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asInt(runtime.pop()), 58);
}
test "sub_u8" {
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [5]u8 = [_]u8{ @intFromEnum(Op.push_u8_literal), 100, @intFromEnum(Op.push_u8_literal), 42, @intFromEnum(Op.sub_u8) };
    try runBytecode(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asChar(runtime.pop()), 58);
}

test "mul_f32" {
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [11]u8 = [_]u8{ @intFromEnum(Op.push_f32_literal), 66, 200, 0, 0, @intFromEnum(Op.push_f32_literal), 66, 40, 0, 0, @intFromEnum(Op.mul_f32) };
    try runBytecode(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asFloat(runtime.pop()), 4200.0);
}
test "mul_i32" {
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [11]u8 = [_]u8{ @intFromEnum(Op.push_i32_literal), 0, 0, 0, 100, @intFromEnum(Op.push_i32_literal), 0, 0, 0, 42, @intFromEnum(Op.mul_i32) };
    try runBytecode(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asInt(runtime.pop()), 4200);
}
test "mul_u8" {
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [5]u8 = [_]u8{ @intFromEnum(Op.push_u8_literal), 80, @intFromEnum(Op.push_u8_literal), 2, @intFromEnum(Op.mul_u8) };
    try runBytecode(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asChar(runtime.pop()), 160);
}

test "div_f32" {
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [11]u8 = [_]u8{ @intFromEnum(Op.push_f32_literal), 66, 200, 0, 0, @intFromEnum(Op.push_f32_literal), 66, 40, 0, 0, @intFromEnum(Op.div_f32) };
    try runBytecode(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asFloat(runtime.pop()), 2.3809524);
}

test "div_i32" {
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [11]u8 = [_]u8{ @intFromEnum(Op.push_i32_literal), 0, 0, 0, 100, @intFromEnum(Op.push_i32_literal), 0, 0, 0, 42, @intFromEnum(Op.div_i32) };
    try runBytecode(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asInt(runtime.pop()), 2);
}

test "div_u8" {
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [5]u8 = [_]u8{ @intFromEnum(Op.push_u8_literal), 100, @intFromEnum(Op.push_u8_literal), 42, @intFromEnum(Op.div_u8) };
    try runBytecode(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asChar(runtime.pop()), 2);
}

test "mod_i32" {
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [11]u8 = [_]u8{ @intFromEnum(Op.push_i32_literal), 0, 0, 0, 100, @intFromEnum(Op.push_i32_literal), 0, 0, 0, 42, @intFromEnum(Op.mod_i32) };
    try runBytecode(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asInt(runtime.pop()), 16);
}

test "pow_f32" {
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [11]u8 = [_]u8{ @intFromEnum(Op.push_f32_literal), 64, 0, 0, 0, @intFromEnum(Op.push_f32_literal), 65, 256 - 128, 0, 0, @intFromEnum(Op.pow_f32) };
    try runBytecode(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asFloat(runtime.pop()), 65536.0);
}

test "pow_i32" {
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [11]u8 = [_]u8{ @intFromEnum(Op.push_i32_literal), 0, 0, 0, 2, @intFromEnum(Op.push_i32_literal), 0, 0, 0, 16, @intFromEnum(Op.pow_i32) };
    try runBytecode(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asInt(runtime.pop()), 65536);
}

test "eq_bool" {
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [3]u8 = [_]u8{ @intFromEnum(Op.push_bool_false), @intFromEnum(Op.push_bool_true), @intFromEnum(Op.eq_bool) };
    try runBytecode(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asBool(runtime.pop()), false);
}

test "eq_f32" {
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [11]u8 = [_]u8{ @intFromEnum(Op.push_f32_literal), 66, 200, 0, 0, @intFromEnum(Op.push_f32_literal), 66, 200, 0, 0, @intFromEnum(Op.eq_f32) };
    try runBytecode(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asBool(runtime.pop()), true);
}

test "eq_i32" {
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [11]u8 = [_]u8{ @intFromEnum(Op.push_i32_literal), 0, 0, 0, 100, @intFromEnum(Op.push_i32_literal), 0, 0, 0, 100, @intFromEnum(Op.eq_i32) };
    try runBytecode(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asBool(runtime.pop()), true);
}

test "eq_string" {
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [21]u8 = [_]u8{ @intFromEnum(Op.push_string_literal), 0, 0, 0, 5, 'h', 'e', 'l', 'l', 'o', @intFromEnum(Op.push_string_literal), 0, 0, 0, 5, 'h', 'e', 'l', 'l', 'o', @intFromEnum(Op.eq_string) };
    try runBytecode(&bc, &runtime);

    try std.testing.expect(Pointer.asBool(runtime.peek()));
}

test "eq_u8" {
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [5]u8 = [_]u8{ @intFromEnum(Op.push_u8_literal), 100, @intFromEnum(Op.push_u8_literal), 100, @intFromEnum(Op.eq_u8) };
    try runBytecode(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asBool(runtime.pop()), true);
}

test "neq_bool" {
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [3]u8 = [_]u8{ @intFromEnum(Op.push_bool_true), @intFromEnum(Op.push_bool_false), @intFromEnum(Op.neq_bool) };
    try runBytecode(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asBool(runtime.pop()), true);
}

test "neq_f32" {
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [11]u8 = [_]u8{ @intFromEnum(Op.push_f32_literal), 66, 200, 0, 0, @intFromEnum(Op.push_f32_literal), 66, 40, 0, 0, @intFromEnum(Op.neq_f32) };
    try runBytecode(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asBool(runtime.pop()), true);
}

test "neq_i32" {
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [11]u8 = [_]u8{ @intFromEnum(Op.push_i32_literal), 0, 0, 0, 100, @intFromEnum(Op.push_i32_literal), 0, 0, 0, 42, @intFromEnum(Op.neq_i32) };
    try runBytecode(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asBool(runtime.pop()), true);
}

test "neq_string" {
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [21]u8 = [_]u8{ @intFromEnum(Op.push_string_literal), 0, 0, 0, 5, 'h', 'e', 'l', 'l', 'o', @intFromEnum(Op.push_string_literal), 0, 0, 0, 5, 'w', 'o', 'r', 'l', 'd', @intFromEnum(Op.neq_string) };
    try runBytecode(&bc, &runtime);

    try std.testing.expect(Pointer.asBool(runtime.peek()));
}

test "neq_u8" {
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [5]u8 = [_]u8{ @intFromEnum(Op.push_u8_literal), 100, @intFromEnum(Op.push_u8_literal), 42, @intFromEnum(Op.neq_u8) };
    try runBytecode(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asBool(runtime.pop()), true);
}

test "lt_f32" {
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [11]u8 = [_]u8{ @intFromEnum(Op.push_f32_literal), 66, 40, 0, 0, @intFromEnum(Op.push_f32_literal), 66, 200, 0, 0, @intFromEnum(Op.lt_f32) };
    try runBytecode(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asBool(runtime.pop()), true);
}

test "lt_i32" {
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [11]u8 = [_]u8{ @intFromEnum(Op.push_i32_literal), 0, 0, 0, 42, @intFromEnum(Op.push_i32_literal), 0, 0, 0, 100, @intFromEnum(Op.lt_i32) };
    try runBytecode(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asBool(runtime.pop()), true);
}

test "lt_string" {
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [21]u8 = [_]u8{ @intFromEnum(Op.push_string_literal), 0, 0, 0, 5, 'h', 'e', 'l', 'l', 'o', @intFromEnum(Op.push_string_literal), 0, 0, 0, 5, 'w', 'o', 'r', 'l', 'd', @intFromEnum(Op.lt_string) };
    try runBytecode(&bc, &runtime);

    try std.testing.expect(Pointer.asBool(runtime.peek()));
}

test "lt_u8" {
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [5]u8 = [_]u8{ @intFromEnum(Op.push_u8_literal), 42, @intFromEnum(Op.push_u8_literal), 100, @intFromEnum(Op.lt_u8) };
    try runBytecode(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asBool(runtime.pop()), true);
}

test "le_f32" {
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [11]u8 = [_]u8{ @intFromEnum(Op.push_f32_literal), 66, 40, 0, 0, @intFromEnum(Op.push_f32_literal), 66, 200, 0, 0, @intFromEnum(Op.le_f32) };
    try runBytecode(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asBool(runtime.pop()), true);
}

test "le_i32" {
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [11]u8 = [_]u8{ @intFromEnum(Op.push_i32_literal), 0, 0, 0, 42, @intFromEnum(Op.push_i32_literal), 0, 0, 0, 100, @intFromEnum(Op.le_i32) };
    try runBytecode(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asBool(runtime.pop()), true);
}

test "le_string" {
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [21]u8 = [_]u8{ @intFromEnum(Op.push_string_literal), 0, 0, 0, 5, 'h', 'e', 'l', 'l', 'o', @intFromEnum(Op.push_string_literal), 0, 0, 0, 5, 'w', 'o', 'r', 'l', 'd', @intFromEnum(Op.le_string) };
    try runBytecode(&bc, &runtime);

    try std.testing.expect(Pointer.asBool(runtime.peek()));
}

test "le_u8" {
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [5]u8 = [_]u8{ @intFromEnum(Op.push_u8_literal), 42, @intFromEnum(Op.push_u8_literal), 100, @intFromEnum(Op.le_u8) };
    try runBytecode(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asBool(runtime.pop()), true);
}

test "gt_f32" {
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [11]u8 = [_]u8{ @intFromEnum(Op.push_f32_literal), 66, 200, 0, 0, @intFromEnum(Op.push_f32_literal), 66, 40, 0, 0, @intFromEnum(Op.gt_f32) };
    try runBytecode(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asBool(runtime.pop()), true);
}

test "gt_i32" {
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [11]u8 = [_]u8{ @intFromEnum(Op.push_i32_literal), 0, 0, 0, 100, @intFromEnum(Op.push_i32_literal), 0, 0, 0, 42, @intFromEnum(Op.gt_i32) };
    try runBytecode(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asBool(runtime.pop()), true);
}

test "gt_string" {
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [21]u8 = [_]u8{ @intFromEnum(Op.push_string_literal), 0, 0, 0, 5, 'h', 'e', 'l', 'l', 'o', @intFromEnum(Op.push_string_literal), 0, 0, 0, 5, 'w', 'o', 'r', 'l', 'd', @intFromEnum(Op.gt_string) };
    try runBytecode(&bc, &runtime);

    try std.testing.expect(!Pointer.asBool(runtime.peek()));
}

test "gt_u8" {
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [5]u8 = [_]u8{ @intFromEnum(Op.push_u8_literal), 100, @intFromEnum(Op.push_u8_literal), 42, @intFromEnum(Op.gt_u8) };
    try runBytecode(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asBool(runtime.pop()), true);
}

test "ge_f32" {
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [11]u8 = [_]u8{ @intFromEnum(Op.push_f32_literal), 66, 200, 0, 0, @intFromEnum(Op.push_f32_literal), 66, 40, 0, 0, @intFromEnum(Op.ge_f32) };
    try runBytecode(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asBool(runtime.pop()), true);
}

test "ge_i32" {
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [11]u8 = [_]u8{ @intFromEnum(Op.push_i32_literal), 0, 0, 0, 100, @intFromEnum(Op.push_i32_literal), 0, 0, 0, 42, @intFromEnum(Op.ge_i32) };
    try runBytecode(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asBool(runtime.pop()), true);
}

test "ge_string" {
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [21]u8 = [_]u8{ @intFromEnum(Op.push_string_literal), 0, 0, 0, 5, 'h', 'e', 'l', 'l', 'o', @intFromEnum(Op.push_string_literal), 0, 0, 0, 5, 'w', 'o', 'r', 'l', 'd', @intFromEnum(Op.ge_string) };
    try runBytecode(&bc, &runtime);

    try std.testing.expect(!Pointer.asBool(runtime.peek()));
}

test "ge_u8" {
    var runtime = try Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [5]u8 = [_]u8{ @intFromEnum(Op.push_u8_literal), 100, @intFromEnum(Op.push_u8_literal), 42, @intFromEnum(Op.ge_u8) };
    try runBytecode(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asBool(runtime.pop()), true);
}

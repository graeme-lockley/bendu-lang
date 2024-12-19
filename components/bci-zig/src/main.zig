const std = @import("std");

const Interpreter = @import("interpreter.zig");
const Runtime = @import("runtime.zig");

const stdout = std.io.getStdOut().writer();

pub fn main() !void {
    var gpa = std.heap.GeneralPurposeAllocator(.{}){};
    const allocator = gpa.allocator();
    defer {
        const err = gpa.deinit();
        if (err == std.heap.Check.leak) {
            stdout.print("Failed to deinit allocator\n", .{}) catch {};
            std.process.exit(1);
        }
    }

    const args = try std.process.argsAlloc(allocator);
    defer std.process.argsFree(allocator, args);

    if (args.len == 2) {
        try runInterpreter(allocator, args[1]);
    } else {
        try stdout.print("Usage: {s} bc-script\n", .{args[0]});
    }
}

fn runInterpreter(allocator: std.mem.Allocator, fileName: []const u8) !void {
    var runtime = try Runtime.Runtime.init(allocator);
    defer runtime.deinit();

    const fileNameSP = try runtime.sp.intern(fileName);
    defer fileNameSP.decRef();

    const package = try runtime.packages.load(fileNameSP);
    try Interpreter.run(package, &runtime);
}

test "tests" {
    _ = @import("pointer.zig");
    _ = @import("interpreter.zig");
}
